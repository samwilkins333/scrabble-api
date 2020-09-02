package com.swilkins.scrabbleapi;

import com.sun.jdi.connect.Connector;
import com.swilkins.scrabbleapi.debug.DebugClassLocation;
import com.swilkins.scrabbleapi.debug.DebugClassSource;
import com.swilkins.scrabbleapi.debug.Debugger;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FibonacciDebugger extends Debugger {

  public FibonacciDebugger() throws Exception {
    super(Fibonacci.class, 10);
  }

  public static final Map<String, Object> dereferencedVariables = new HashMap<>();

  @Override
  protected void configureDebuggerModel() {
    debuggerModel.addDebugClassSource(Fibonacci.class, new DebugClassSource() {
      @Override
      public String getContentsAsString() throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream("/src/Fibonacci.java"), StandardCharsets.UTF_8);
      }
    });
  }

  @Override
  protected void configureVirtualMachineLaunch(Map<String, Connector.Argument> arguments) {
    arguments.get("options").setValue("-cp target/classes");
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
