package com.swilkins.scrabbleapi;

import com.sun.jdi.connect.Connector;
import com.swilkins.scrabbleapi.debug.DebugClassSource;
import com.swilkins.scrabbleapi.debug.Debugger;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class ScrabbleApiApplication extends Debugger {

  public static final Map<String, Object> dereferencedVariables = new HashMap<>();

  public ScrabbleApiApplication() throws Exception {
    super(Fibonacci.class, 10);
  }

  public static void main(String[] args) throws Exception {
    SpringApplication.run(ScrabbleApiApplication.class, args);
    new ScrabbleApiApplication();
  }


  @Override
  protected void configureDebuggerModel() {
    debuggerModel.addDebugClassSource(Fibonacci.class, new DebugClassSource(true, 11) {
      @Override
      public String getContentsAsString() throws Exception {
        return IOUtils.toString(getClass().getResourceAsStream("/src/Fibonacci.java"), StandardCharsets.UTF_8);
      }
    });
  }

  @Override
  protected void configureVirtualMachineLaunch(Map<String, Connector.Argument> arguments) {
    arguments.get("options").setValue("-cp target/classes");
  }

}
