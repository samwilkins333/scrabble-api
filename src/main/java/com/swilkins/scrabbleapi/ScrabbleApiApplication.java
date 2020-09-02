package com.swilkins.scrabbleapi;

import com.sun.jdi.*;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.event.*;
import com.swilkins.scrabbleapi.debug.DebugClassSource;
import com.swilkins.scrabbleapi.debug.DebuggerModel;
import com.swilkins.scrabbleapi.debug.Dereferencer;
import com.swilkins.scrabbleapi.executable.Fibonacci;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.swilkins.scrabbleapi.utility.Utilities.inputStreamToString;

@SpringBootApplication
public class ScrabbleApiApplication {

  public static final Map<String, Object> dereferencedVariables = new HashMap<>();
  private static VirtualMachine virtualMachine;
  private static final DebuggerModel debuggerModel = new DebuggerModel();

  private static final Map<Class<?>, Dereferencer> dereferencerMap = new HashMap<>();
  private static final Dereferencer toString = (object, thread) -> standardDereference(object, "toString", thread);

  public static void main(String[] args) {
    SpringApplication.run(ScrabbleApiApplication.class, args);

    LaunchingConnector launchingConnector = Bootstrap.virtualMachineManager().defaultConnector();
    Map<String, Connector.Argument> arguments = launchingConnector.defaultArguments();
    StringBuilder main = new StringBuilder(Fibonacci.class.getName());
    for (Object virtualMachineArgument : args) {
      main.append(" ").append(virtualMachineArgument);
    }
    arguments.get("main").setValue(main.toString());

    try {
      virtualMachine = launchingConnector.launch(arguments);
      debuggerModel.setEventRequestManager(virtualMachine.eventRequestManager());

      debuggerModel.addDebugClassSource(Fibonacci.class, new DebugClassSource(true, 14, 18) {
        @Override
        public String getContentsAsString() {
          return inputStreamToString(getClass().getResourceAsStream("executable/Fibonacci.java"));
        }
      });

      debuggerModel.submitDebugClassSources();
      debuggerModel.enableExceptionReporting(true, true);

      EventSet eventSet;
      while ((eventSet = virtualMachine.eventQueue().remove()) != null) {
        for (Event event : eventSet) {
          if (event instanceof ClassPrepareEvent) {
            debuggerModel.createDebugClassFrom((ClassPrepareEvent) event);
          } else if (event instanceof ExceptionEvent) {
            System.out.println(dereferenceValue(((ExceptionEvent) event).thread(), ((ExceptionEvent) event).exception()));
          } else if (event instanceof LocatableEvent) {
            LocatableEvent locatableEvent = (LocatableEvent) event;
            if (debuggerModel.toDebugClassLocation(locatableEvent.location()) != null) {
              dereferenceVariables(locatableEvent.thread());
              synchronized (dereferencedVariables) {
                dereferencedVariables.notifyAll();
                try {
                  dereferencedVariables.wait();
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
              }
            }
          }
        }
        virtualMachine.resume();
      }
    } catch (VMDisconnectedException e) {
      Process process = virtualMachine.process();
      String virtualMachineOut = inputStreamToString(process.getInputStream());
      String virtualMachineError = inputStreamToString(process.getErrorStream());
      System.out.println(virtualMachineOut);
      System.out.println(virtualMachineError);
      System.exit(0);
    } catch (NoSuchMethodException e) {
      System.out.println(e.getMessage());
      System.exit(1);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void dereferenceVariables(ThreadReference thread)
          throws Exception {
    StackFrame frame = thread.frame(0);
    dereferencedVariables.clear();
    Map<LocalVariable, Value> values = frame.getValues(frame.visibleVariables());
    debuggerModel.deadlockSafeInvoke(() -> {
      for (Map.Entry<LocalVariable, Value> entry : values.entrySet()) {
        dereferencedVariables.put(entry.getKey().name(), dereferenceValue(thread, entry.getValue()));
      }
    });
  }

  private static Object standardDereference(ObjectReference value, String toInvokeName, ThreadReference thread) throws NoSuchMethodException {
    return dereferenceValue(thread, invoke(value, thread, toInvokeName, null, null));
  }

  private static Object dereferenceValue(ThreadReference thread, Value value) throws NoSuchMethodException {
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

  private static Value invoke(ObjectReference object, ThreadReference thread, String toInvokeName, String signature, List<? extends Value> arguments) throws NoSuchMethodException {
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

  private static Dereferencer getDereferencerFor(ObjectReference objectReference) {
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

}
