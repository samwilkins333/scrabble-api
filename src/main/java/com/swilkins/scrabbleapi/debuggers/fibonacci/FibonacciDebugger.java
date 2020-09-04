package com.swilkins.scrabbleapi.debuggers.fibonacci;

import com.swilkins.scrabbleapi.debug.Debugger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class FibonacciDebugger extends Debugger {

  public FibonacciDebugger() throws IllegalArgumentException, IOException, ClassNotFoundException {
    super();
  }

  @Override
  protected void onVirtualMachineSuspension() {
    System.out.println(suspendedLocation);
    for (Map.Entry<String, Object> entry : dereferencedVariables.entrySet()) {
      String variable = entry.getKey();
      Object value = entry.getValue();
      String valueString = value.getClass().isArray() ? Arrays.deepToString((Object[]) value) : value.toString();
      System.out.printf("%s=%s\n", variable, valueString);
    }
  }

}
