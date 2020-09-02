package com.swilkins.scrabbleapi;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.ClassPrepareRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class ScrabbleApiApplication {

  public static final Map<String, Object> dereferencedVariables = new HashMap<>();

  static {
    dereferencedVariables.put("status", "Uninitialized.");
  }

  public static void main(String[] args) {
    SpringApplication.run(ScrabbleApiApplication.class, args);

    LaunchingConnector launchingConnector = Bootstrap.virtualMachineManager().defaultConnector();
    Map<String, Connector.Argument> arguments = launchingConnector.defaultArguments();
    arguments.get("main").setValue(Fibonacci.class.getName() + " " + args[0]);
    try {
      VirtualMachine virtualMachine = launchingConnector.launch(arguments);
      virtualMachine.eventRequestManager().createExceptionRequest(null, true, true).enable();
      ClassPrepareRequest classPrepareRequest = virtualMachine.eventRequestManager().createClassPrepareRequest();
      classPrepareRequest.addClassFilter(Fibonacci.class.getName());
      classPrepareRequest.enable();
      EventSet eventSet;
      while ((eventSet = virtualMachine.eventQueue().remove()) != null) {
        for (Event event : eventSet) {
          if (event instanceof ClassPrepareEvent) {
            virtualMachine.eventRequestManager().createBreakpointRequest(((ClassPrepareEvent) event).referenceType().locationsOfLine(11).get(0)).enable();
          }
          System.out.println(event);
        }
        virtualMachine.resume();
      }
    } catch (VMDisconnectedException ignored) {
    } catch (IOException | VMStartException | IllegalConnectorArgumentsException | InterruptedException | AbsentInformationException e) {
      e.printStackTrace();
    }
//    int i = 0;
//    while (true) {
//      synchronized (dereferencedVariables) {
//        try {
//          dereferencedVariables.wait();
//        } catch (InterruptedException e) {
//          e.printStackTrace();
//        }
//      }
//      for (int j = 0; j < 10e8; j++) {
//        i++;
//      }
//      synchronized (dereferencedVariables) {
//        dereferencedVariables.put("status", String.format("Finished work. i = %d", i));
//        dereferencedVariables.notifyAll();
//      }
//    }
  }


}
