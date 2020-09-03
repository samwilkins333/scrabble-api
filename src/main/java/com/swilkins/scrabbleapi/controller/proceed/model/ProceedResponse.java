package com.swilkins.scrabbleapi.controller.proceed.model;

import java.util.Map;

public class ProceedResponse {

  public Map<String, Object> dereferencedVariables;
  public Location location;

  public static class Location {
    public String className;
    public int lineNumber;
  }

}
