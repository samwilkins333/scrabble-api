package com.swilkins.scrabbleapi.debuggers.generator;

import com.sun.jdi.connect.Connector;
import com.swilkins.scrabbleapi.debug.Debugger;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GeneratorDebugger extends Debugger {
  private static final String jarPathCore = "/lib/scrabble-base-0.0.1.jar";

  public GeneratorDebugger() throws IllegalArgumentException, IOException, ClassNotFoundException {
    super();
  }

  @Override
  protected void configureDebuggerModel() throws IOException, ClassNotFoundException  {
    super.configureDebuggerModel();
    debuggerModel.addDebugClassSourcesFromJar(getClass().getResource(jarPathCore).getPath(), null);
  }

  @Override
  protected void configureVirtualMachineLaunch(Map<String, Connector.Argument> arguments, List<String> classPathEntries) {
    super.configureVirtualMachineLaunch(arguments, classPathEntries);
    classPathEntries.add("src/main/resources" + jarPathCore);
  }

  @Override
  protected void onVirtualMachineSuspension() {

  }

}
