package com.swilkins.scrabbleapi;

import com.swilkins.scrabbleapi.debug.Debugger;
import com.swilkins.scrabbleapi.debuggers.fibonacci.FibonacciDebugger;
import com.swilkins.scrabbleapi.debuggers.generator.GeneratorDebugger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class JDIDebuggerServer {
  private static Debugger debugger;

  public static Debugger getDebugger() {
    return debugger;
  }

  public static void main(String[] args) {
    SpringApplication.run(JDIDebuggerServer.class, args);
    try {
//      (debugger = new FibonacciDebugger()).start();
      (debugger = new GeneratorDebugger()).start();
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }
  }

}
