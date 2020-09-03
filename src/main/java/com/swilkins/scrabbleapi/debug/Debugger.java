package com.swilkins.scrabbleapi.debug;

import com.sun.jdi.*;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.event.*;
import com.sun.jdi.request.StepRequest;
import com.swilkins.scrabbleapi.debug.interfaces.DebugClassSource;
import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public abstract class Debugger {

  protected VirtualMachine virtualMachine;

  protected final DebuggerModel debuggerModel;
  protected final Map<DebugClassSource, Class<?>> scannedDebugClassSources = new HashMap<>();

  private final String packageName;
  private final Class<?> virtualMachineTargetClass;
  private final String[] virtualMachineArguments;

  protected final Map<Class<?>, Dereferencer> dereferencerMap = new HashMap<>();
  protected final Dereferencer toString = (object, thread) -> standardDereference(object, "toString", thread);

  private final Object eventProcessingControl = new Object();
  private final Object stepRequestControl = new Object();
  private final Object threadReferenceControl = new Object();

  private Integer activeStepRequestDepth;
  private Integer requestedStepRequestDepth;
  private final Map<Integer, StepRequest> stepRequestMap = new HashMap<>(3);

  protected final Map<String, Object> dereferencedVariables = new HashMap<>();
  protected DebugClassLocation suspendedLocation;

  public Debugger() throws IllegalArgumentException {
    debuggerModel = new DebuggerModel();

    Class<?> thisClass = getClass();
    DebugClassSource main = null;
    Reflections reflections = new Reflections(packageName = thisClass.getPackageName());
    for (Class<?> sourceClass : reflections.getTypesAnnotatedWith(DebugClassSource.class)) {
      DebugClassSource annotation = sourceClass.getAnnotation(DebugClassSource.class);
      for (Class<?> debuggerClass : annotation.debuggerClasses()) {
        if (!debuggerClass.equals(thisClass)) {
          continue;
        }
        scannedDebugClassSources.put(annotation, sourceClass);
        if (annotation.main()) {
          if (main != null) {
            throw new IllegalArgumentException("Cannot specify more than one main class.");
          }
          main = annotation;
        }
        break;
      }
    }

    if (main == null) {
      throw new IllegalArgumentException("Must specify at least one main class.");
    }

    virtualMachineTargetClass = scannedDebugClassSources.get(main);
    virtualMachineArguments = main.args();

    configureDebuggerModel();
    configureDereferencers();
  }

  public void start() {
    LaunchingConnector launchingConnector = Bootstrap.virtualMachineManager().defaultConnector();
    Map<String, Connector.Argument> arguments = launchingConnector.defaultArguments();
    StringBuilder main = new StringBuilder(virtualMachineTargetClass.getName());
    for (Object virtualMachineArgument : virtualMachineArguments) {
      main.append(" ").append(virtualMachineArgument);
    }
    arguments.get("main").setValue(main.toString());

    configureVirtualMachineLaunch(arguments);

    try {
      virtualMachine = launchingConnector.launch(arguments);
      debuggerModel.setEventRequestManager(virtualMachine.eventRequestManager());
      debuggerModel.submitDebugClassSources();
      debuggerModel.enableExceptionReporting(true, true);

      EventSet eventSet;
      while ((eventSet = virtualMachine.eventQueue().remove()) != null) {
        for (Event event : eventSet) {
          if (event instanceof ClassPrepareEvent) {
            debuggerModel.createDebugClassFrom((ClassPrepareEvent) event);
          } else if (event instanceof ExceptionEvent) {
            ExceptionEvent exceptionEvent = (ExceptionEvent) event;
            System.out.println(dereferenceValue(exceptionEvent.thread(), exceptionEvent.exception()));
          } else if (event instanceof LocatableEvent) {
            onVirtualMachineLocatableEvent((LocatableEvent) event, eventSet.size());
            dereferencedVariables.clear();
            suspendedLocation = null;
          }
        }
        virtualMachine.resume();
      }
    } catch (VMDisconnectedException e) {
      Process process = virtualMachine.process();
      try {
        String virtualMachineOut = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        String virtualMachineError = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
        onVirtualMachineTermination(virtualMachineOut, virtualMachineError);
        synchronized (eventProcessingControl) {
          eventProcessingControl.notifyAll();
        }
      } catch (IOException ignored) {
      }
    } catch (NoSuchMethodException e) {
      System.out.println(e.getMessage());
      System.exit(1);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void configureDebuggerModel() {
    for (Map.Entry<DebugClassSource, Class<?>> entry : scannedDebugClassSources.entrySet()) {
      DebugClassSource annotation = entry.getKey();
      Class<?> clazz = entry.getValue();
      String internalPath = clazz.getName().replace(packageName, "").substring(1).replace(".", "/");
      String sourcePath = String.format("/src/%s.java", internalPath);
      com.swilkins.scrabbleapi.debug.DebugClassSource debugClassSource = new com.swilkins.scrabbleapi.debug.DebugClassSource(() -> {
        InputStream sourceStream = getClass().getResourceAsStream(sourcePath);
        return IOUtils.toString(sourceStream, StandardCharsets.UTF_8);
      });
      debugClassSource.addCompileTimeBreakpoints(annotation.compileTimeBreakpoints());
      debugClassSource.setCached(annotation.cached());
      debuggerModel.addDebugClassSource(clazz, debugClassSource);
    }
  }

  protected void configureDereferencers() {
    dereferencerMap.put(AbstractCollection.class, (extendsAbstractCollection, thread) -> standardDereference(extendsAbstractCollection, "toArray", thread));
  }

  protected void configureVirtualMachineLaunch(Map<String, Connector.Argument> arguments) {
    arguments.get("options").setValue("-cp target/classes");
  }

  protected void onVirtualMachineLocatableEvent(LocatableEvent event, int eventSetSize) throws Exception {
    DebugClassLocation location = debuggerModel.toDebugClassLocation(event.location());
    if (location == null || eventSetSize > 1) {
      return;
    }
    ThreadReference thread = event.thread();
    dereferenceVariables(thread);
    suspendedLocation = location;
    onVirtualMachineSuspension();
    signalAndAwaitCoroutine();
    respondToRequestedStepRequestDepth(thread);
  }

  public void setRequestedStepRequestDepth(Integer requestedStepRequestDepth) {
    this.requestedStepRequestDepth = requestedStepRequestDepth;
  }

  public void respondToRequestedStepRequestDepth(ThreadReference threadReference) {
    synchronized (stepRequestControl) {
      disableActiveStepRequest();
      if (requestedStepRequestDepth == null) {
        return;
      }
      if (activeStepRequestDepth == null || !activeStepRequestDepth.equals(requestedStepRequestDepth)) {
        StepRequest requestedStepRequest = stepRequestMap.get(requestedStepRequestDepth);
        if (requestedStepRequest == null) {
          synchronized (threadReferenceControl) {
            requestedStepRequest = debuggerModel.createStepRequest(threadReference, requestedStepRequestDepth);
          }
          stepRequestMap.put(requestedStepRequestDepth, requestedStepRequest);
        }
        debuggerModel.setEventRequestEnabled(requestedStepRequest, true);
        activeStepRequestDepth = requestedStepRequestDepth;
      }
    }
  }

  private void disableActiveStepRequest() {
    synchronized (stepRequestControl) {
      if (activeStepRequestDepth != null) {
        StepRequest activeStepRequest = stepRequestMap.get(activeStepRequestDepth);
        if (activeStepRequest != null) {
          debuggerModel.setEventRequestEnabled(activeStepRequest, false);
        }
        activeStepRequestDepth = null;
      }
    }
  }

  protected abstract void onVirtualMachineSuspension();

  protected void onVirtualMachineTermination(String virtualMachineOut, String virtualMachineError) {
    if (virtualMachineOut != null && !virtualMachineOut.isEmpty()) {
      System.out.println(virtualMachineOut);
    }
    if (virtualMachineError != null && !virtualMachineError.isEmpty()) {
      System.out.println(virtualMachineError);
    }
  }

  private Dereferencer getDereferencerFor(ObjectReference objectReference) {
    Dereferencer dereferencer = toString;
    try {
      Class<?> clazz = Class.forName(objectReference.referenceType().name());
      while (clazz != null) {
        Dereferencer existing = dereferencerMap.get(clazz);
        if (existing != null) {
          dereferencer = existing;
          break;
        }
        clazz = clazz.getSuperclass();
      }
    } catch (ClassNotFoundException ignored) {
    }
    return dereferencer;
  }

  public void signalAndAwaitCoroutine() {
    synchronized (eventProcessingControl) {
      try {
        eventProcessingControl.notifyAll();
        eventProcessingControl.wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  protected Value invoke(ObjectReference object, ThreadReference thread, String toInvokeName, String signature, List<? extends Value> arguments) throws NoSuchMethodException {
    ReferenceType referenceType = object.referenceType();
    List<Method> candidates;
    if (signature != null) {
      candidates = referenceType.methodsByName(toInvokeName, signature);
    } else {
      candidates = referenceType.methodsByName(toInvokeName);
    }
    Function<String, NoSuchMethodException> errorMessageBuilder = s -> new NoSuchMethodException(String.format(
            "Illegal method invocation in dereferencer: [%s (%s) invoked on %s]: %s.",
            toInvokeName, signature, referenceType.name(), s
    ));
    if (candidates.isEmpty()) {
      throw errorMessageBuilder.apply("Method does not exist");
    }
    try {
      Method toInvoke = candidates.get(0);
      if (arguments == null) {
        arguments = Collections.emptyList();
      }
      return object.invokeMethod(thread, toInvoke, arguments, 0);
    } catch (Exception e) {
      throw errorMessageBuilder.apply(e.getMessage());
    }
  }

  private void dereferenceVariables(ThreadReference thread)
          throws Exception {
    StackFrame frame = thread.frame(0);
    Map<LocalVariable, Value> values = frame.getValues(frame.visibleVariables());
    debuggerModel.deadlockSafeInvoke(() -> {
      for (Map.Entry<LocalVariable, Value> entry : values.entrySet()) {
        dereferencedVariables.put(entry.getKey().name(), dereferenceValue(thread, entry.getValue()));
      }
    });
  }

  public Map<String, Object> getDereferencedVariables() {
    return Collections.unmodifiableMap(dereferencedVariables);
  }

  protected Object standardDereference(ObjectReference value, String toInvokeName, ThreadReference thread) throws NoSuchMethodException {
    return dereferenceValue(thread, invoke(value, thread, toInvokeName, null, null));
  }

  protected Object dereferenceValue(ThreadReference thread, Value value) throws NoSuchMethodException {
    if (value instanceof ObjectReference) {
      ((ObjectReference) value).disableCollection();
    }
    if (value instanceof ArrayReference) {
      ArrayReference arrayReference = (ArrayReference) value;
      int length = arrayReference.length();
      Object[] collector = new Object[length];
      for (int i = 0; i < length; i++) {
        collector[i] = dereferenceValue(thread, arrayReference.getValue(i));
      }
      return collector;
    } else if (value instanceof StringReference) {
      return ((StringReference) value).value();
    } else if (value instanceof ObjectReference) {
      ObjectReference objectReference = (ObjectReference) value;
      return getDereferencerFor(objectReference).dereference(objectReference, thread);
    } else if (value instanceof PrimitiveValue) {
      PrimitiveValue primitiveValue = (PrimitiveValue) value;
      String subType = value.type().name();
      if (subType.equals("char")) {
        return primitiveValue.charValue();
      }
      if (subType.equals("boolean")) {
        return primitiveValue.booleanValue();
      }
      if (subType.equals("byte")) {
        return primitiveValue.byteValue();
      }
      if (subType.equals("double")) {
        return primitiveValue.doubleValue();
      }
      if (subType.equals("float")) {
        return primitiveValue.floatValue();
      }
      if (subType.equals("int")) {
        return primitiveValue.intValue();
      }
      if (subType.equals("long")) {
        return primitiveValue.longValue();
      }
      if (subType.equals("short")) {
        return primitiveValue.shortValue();
      }
    }
    return value;
  }

  public DebugClassLocation getSuspendedLocation() {
    return suspendedLocation;
  }

}