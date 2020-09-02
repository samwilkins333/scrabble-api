package com.swilkins.scrabbleapi.executable;

import com.sun.jdi.connect.Connector;
import com.swilkins.scrabbleapi.debug.DebugClassSource;
import com.swilkins.scrabbleapi.debug.Debugger;

import java.util.Map;

import static com.swilkins.scrabbleapi.utility.Utilities.inputStreamToString;

public class FibonacciDebugger extends Debugger {

  public FibonacciDebugger(int index) throws Exception {
    super(Fibonacci.class, null, index);
  }

  @Override
  protected void configureDebuggerModel() {
    debuggerModel.addDebugClassSource(Fibonacci.class, new DebugClassSource(true, 13) {
      @Override
      public String getContentsAsString() {
        return inputStreamToString(getClass().getResourceAsStream("Fibonacci.java"));
      }
    });
  }

  @Override
  protected void configureDereferencers() {

  }

  @Override
  protected void configureVirtualMachineLaunch(Map<String, Connector.Argument> arguments) {

  }

}
