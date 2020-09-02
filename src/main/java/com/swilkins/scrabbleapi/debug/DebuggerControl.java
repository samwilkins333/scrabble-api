package com.swilkins.scrabbleapi.debug;

public enum DebuggerControl {

  CONTINUE("Continue"),
  STEP_OVER("Step Over"),
  STEP_INTO("Step Into"),
  STEP_OUT("Step Out"),
  TOGGLE_BREAKPOINT("Toggle Breakpoint"),
  RECENTER("Recenter");

  private final String label;

  DebuggerControl(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

}
