package com.swilkins.scrabbleapi.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DebugClassSource {

  private final List<Integer> compileTimeBreakpoints = new ArrayList<>();
  private final DebugSourceSupplier sourceSupplier;
  private boolean cached;

  public DebugClassSource(DebugSourceSupplier sourceSupplier) {
    this.sourceSupplier = sourceSupplier;
  }

  public String getContentsAsString() throws Exception {
    return sourceSupplier.get();
  }

  public List<Integer> getCompileTimeBreakpoints() {
    return Collections.unmodifiableList(compileTimeBreakpoints);
  }

  public void addCompileTimeBreakpoints(int... compileTimeBreakpoints) {
    for (int compileTimeBreakpoint : compileTimeBreakpoints) {
      this.compileTimeBreakpoints.add(compileTimeBreakpoint);
    }
  }

  public boolean isCached() {
    return cached;
  }

  public DebugClassSource setCached(boolean cached) {
    this.cached = cached;
    return this;
  }

}
