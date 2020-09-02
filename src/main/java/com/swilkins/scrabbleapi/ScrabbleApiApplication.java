package com.swilkins.scrabbleapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ScrabbleApiApplication {

  public static void main(String[] args) throws Exception {
    SpringApplication.run(ScrabbleApiApplication.class, args);
    new FibonacciDebugger();
  }

}
