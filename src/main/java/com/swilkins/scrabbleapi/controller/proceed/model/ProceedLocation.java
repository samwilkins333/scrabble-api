package com.swilkins.scrabbleapi.controller.proceed.model;

import java.util.Objects;

public class ProceedLocation {

  public String className;
  public int lineNumber;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProceedLocation that = (ProceedLocation) o;

    if (lineNumber != that.lineNumber) return false;
    return Objects.equals(className, that.className);
  }

  @Override
  public int hashCode() {
    return Objects.hash(className, lineNumber);
  }

  @Override
  public String toString() {
    return "ProceedLocation{" + "className='" + className + '\'' +
            ", lineNumber=" + lineNumber +
            '}';
  }

}
