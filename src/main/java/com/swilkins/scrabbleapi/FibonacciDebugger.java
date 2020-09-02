package com.swilkins.scrabbleapi;

import com.swilkins.scrabbleapi.debug.DebugClassLocation;
import com.swilkins.scrabbleapi.debug.Debugger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FibonacciDebugger extends Debugger {

  public static final Map<String, Object> dereferencedVariables = new HashMap<>();

  public FibonacciDebugger() throws IllegalArgumentException {
    super();
  }

  @Override
  protected void onVirtualMachineSuspension(DebugClassLocation location, Map<String, Object> dereferencedVariables) {
    System.out.println(location);
    for (Map.Entry<String, Object> entry : dereferencedVariables.entrySet()) {
      String variable = entry.getKey();
      Object value = entry.getValue();
      String valueString = value.getClass().isArray() ? Arrays.deepToString((Object[]) value) : value.toString();
      System.out.printf("%s=%s\n", variable, valueString);
    }
  }

  @Override
  protected void onVirtualMachineContinuation() {

  }

}
