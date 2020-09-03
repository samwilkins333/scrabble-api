package com.swilkins.scrabbleapi;

import com.swilkins.scrabbleapi.debug.Debugger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JDIDebuggerServer {
  private static Debugger debugger;

  public static Debugger getDebugger() {
    return debugger;
  }

  public static void main(String[] args) {
    SpringApplication.run(JDIDebuggerServer.class, args);
    debugger = new FibonacciDebugger();
    debugger.start();
  }

}
