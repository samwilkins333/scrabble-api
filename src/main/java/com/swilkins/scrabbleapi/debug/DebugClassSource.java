package com.swilkins.scrabbleapi.debug;

import java.util.ArrayList;
import java.util.List;

public abstract class DebugClassSource {

  private final List<Integer> compileTimeBreakpoints = new ArrayList<>();
  private boolean cached;

  public abstract String getContentsAsString() throws Exception;

  public List<Integer> getCompileTimeBreakpoints() {
    return compileTimeBreakpoints;
  }

  public void addCompileTimeBreakpoints(int... compileTimeBreakpoints) {
    for (int compileTimeBreakpoint : compileTimeBreakpoints) {
      this.compileTimeBreakpoints.add(compileTimeBreakpoint);
    }
  }

  public DebugClassSource setCached(boolean cached) {
    this.cached = cached;
    return this;
  }

  public boolean isCached() {
    return cached;
  }

}
