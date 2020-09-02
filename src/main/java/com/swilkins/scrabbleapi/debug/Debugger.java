package com.swilkins.scrabbleapi.debug;

import com.sun.jdi.*;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.event.*;
import com.swilkins.scrabbleapi.debug.interfaces.DebugClassSource;
import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class Debugger {

  protected VirtualMachine virtualMachine;

  protected final DebuggerModel debuggerModel;
  protected final Map<DebugClassSource, Class<?>> scannedDebugClassSources = new HashMap<>();

  private final Class<?> virtualMachineTargetClass;
  private final String[] virtualMachineArguments;

  protected final Map<Class<?>, Dereferencer> dereferencerMap = new HashMap<>();
  protected final Dereferencer toString = (object, thread) -> standardDereference(object, "toString", thread);

  public Debugger() throws IllegalArgumentException {
    debuggerModel = new DebuggerModel();

    for (Class<?> clazz : new Reflections(getClass().getPackageName()).getTypesAnnotatedWith(DebugClassSource.class)) {
      scannedDebugClassSources.put(clazz.getAnnotation(DebugClassSource.class), clazz);
    }

    List<DebugClassSource> mainSources = scannedDebugClassSources.keySet().stream().filter(DebugClassSource::main).collect(Collectors.toList());
    if (mainSources.size() != 1) {
      throw new IllegalArgumentException();
    }

    DebugClassSource main = mainSources.get(0);
    virtualMachineTargetClass = scannedDebugClassSources.get(main);
    virtualMachineArguments = main.args();

    configureDebuggerModel();

    configureDereferencers();

    this.start();
  }

  private void start() {
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
      com.swilkins.scrabbleapi.debug.DebugClassSource debugClassSource = debuggerModel.addDebugClassSource(entry.getValue(), new com.swilkins.scrabbleapi.debug.DebugClassSource(annotation.compileTimeBreakpoints()) {
        @Override
        public String getContentsAsString() throws Exception {
          return IOUtils.toString(getClass().getResourceAsStream(annotation.sourcePath()), StandardCharsets.UTF_8);
        }
      });
      debugClassSource.setCached(annotation.cached());
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
    onVirtualMachineSuspension(location, dereferenceVariables(thread));
    debuggerModel.awaitEventProcessingContinuation();
    debuggerModel.respondToRequestedStepRequestDepth(thread);
    onVirtualMachineContinuation();
  }

  protected abstract void onVirtualMachineSuspension(DebugClassLocation location, Map<String, Object> dereferencedVariables);

  protected abstract void onVirtualMachineContinuation();

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

  private Map<String, Object> dereferenceVariables(ThreadReference thread)
          throws Exception {
    StackFrame frame = thread.frame(0);
    Map<String, Object> dereferencedVariables = new HashMap<>();
    Map<LocalVariable, Value> values = frame.getValues(frame.visibleVariables());
    debuggerModel.deadlockSafeInvoke(() -> {
      for (Map.Entry<LocalVariable, Value> entry : values.entrySet()) {
        dereferencedVariables.put(entry.getKey().name(), dereferenceValue(thread, entry.getValue()));
      }
    });
    return dereferencedVariables;
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

}