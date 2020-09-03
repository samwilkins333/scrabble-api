package com.swilkins.scrabbleapi.controller.proceed.model;

import java.util.Map;

public class ProceedResponse {

  public Map<String, Object> dereferencedVariables;
  public ProceedLocation updatedLocation;
  public String contentsAsString;

  @Override
  public String toString() {
    return "ProceedResponse{" + "dereferencedVariables=" + dereferencedVariables +
            ", updatedLocation=" + updatedLocation +
            ", contentsAsString='" + contentsAsString + '\'' +
            '}';
  }

}
