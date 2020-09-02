package com.swilkins.scrabbleapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
    int i = 0;
    while (true) {
      synchronized (dereferencedVariables) {
        try {
          dereferencedVariables.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      for (int j = 0; j < 10e8; j++) {
        i++;
      }
      synchronized (dereferencedVariables) {
        dereferencedVariables.put("status", String.format("Finished work. i = %d", i));
        dereferencedVariables.notifyAll();
      }
    }
  }


}
