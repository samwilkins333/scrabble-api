package com.swilkins.scrabbleapi.debug;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.request.*;
import com.swilkins.scrabbleapi.debug.interfaces.Invokable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static com.sun.jdi.request.StepRequest.STEP_LINE;

public class DebuggerModel {

  private static final String JAVA_SUFFIX = ".java";
  private final Map<Class<?>, DebugClassSource> debugClassSources = new LinkedHashMap<>();
  private final Map<Class<?>, DebugClass> debugClasses = new LinkedHashMap<>();
  private final Map<EventRequest, Boolean> eventRequestStateMap = new HashMap<>();
  private String globalClassFilter = null;
  private EventRequestManager eventRequestManager;
  private boolean deadlockSafeInvoke;

  public void setEventRequestManager(EventRequestManager eventRequestManager) {
    this.eventRequestManager = eventRequestManager;
  }

  public void addDebugClassSource(Class<?> clazz, DebugClassSource debugClassSource) {
    debugClassSources.put(clazz, debugClassSource);
  }

  private Class<?> sourceToClass(String source) throws ClassNotFoundException {
    if (source.endsWith(JAVA_SUFFIX)) {
      String entryClass = source.replace("/", ".").replace(JAVA_SUFFIX, "");
      return Class.forName(entryClass);
    }
    return null;
  }

  public Class<?>[] getAllClasses() {
    Class<?>[] representedClasses = new Class<?>[debugClassSources.size()];
    int i = 0;
    for (Map.Entry<Class<?>, DebugClassSource> debugClassSourceEntry : debugClassSources.entrySet()) {
      representedClasses[i++] = debugClassSourceEntry.getKey();
    }
    Arrays.sort(representedClasses, Comparator.comparing(Class::getName));
    return representedClasses;
  }

  public Set<Class<?>> addDebugClassSourcesFromJar(String jarPath, DebugClassSourceFilter filter) throws IOException, ClassNotFoundException {
    File file = new File(jarPath);
    JarFile jarFile = new JarFile(file);

    Enumeration<JarEntry> entries = jarFile.entries();
    List<String> sources = new ArrayList<>();
    while (entries.hasMoreElements()) {
      sources.add(entries.nextElement().getRealName());
    }
    return processSourcesList(sources, filter, sourcePath -> new DebugClassSource(() -> {
      InputStream jarEntryStream = jarFile.getInputStream(jarFile.getEntry(sourcePath));
      return IOUtils.toString(jarEntryStream, StandardCharsets.UTF_8);
    }));
  }

  public Set<Class<?>> addDebugClassesFromDirectory(String directoryPath, DebugClassSourceFilter filter) throws IOException, ClassNotFoundException {
    File directory = new File(directoryPath);
    if (!directory.isDirectory()) {
      throw new IllegalArgumentException();
    }
    List<String> sources = Files.list(directory.toPath()).map(Path::toString).collect(Collectors.toList());
    return processSourcesList(sources, filter, sourcePath -> new DebugClassSource(() -> {
      InputStream fileStream = new FileInputStream(new File(sourcePath));
      return IOUtils.toString(fileStream, StandardCharsets.UTF_8);
    }));
  }

  private Set<Class<?>> processSourcesList(List<String> sources, DebugClassSourceFilter filter, Function<String, DebugClassSource> debugClassSourceProvider) throws ClassNotFoundException {
    Map<Class<?>, String> representedClasses = new HashMap<>();

    for (String source : sources) {
      Class<?> representedClass = sourceToClass(source);
      if (representedClass != null) {
        representedClasses.put(representedClass, source);
      }
    }

    if (filter != null) {
      Set<Class<?>> filteredClasses = filter.getFilteredClasses();
      Set<Class<?>> representedClassesKeySet = representedClasses.keySet();

      if (filter.getFilterType() == DebugClassSourceFilterType.INCLUDE) {
        representedClassesKeySet.retainAll(filteredClasses);
      } else {
        representedClassesKeySet.removeAll(filteredClasses);
      }
    }

    for (Map.Entry<Class<?>, String> representedClass : representedClasses.entrySet()) {
      addDebugClassSource(representedClass.getKey(), debugClassSourceProvider.apply(representedClass.getValue()));
    }

    return representedClasses.keySet();
  }

  public void submitDebugClassSources() {
    String[] classNames = new String[debugClassSources.size()];
    int i = 0;
    for (Class<?> clazz : debugClassSources.keySet()) {
      ClassPrepareRequest request = eventRequestManager.createClassPrepareRequest();
      String className = clazz.getName();
      request.addClassFilter(className);
      classNames[i++] = className;
      setEventRequestEnabled(request, true);
    }
    String globalClassFilter = StringUtils.getCommonPrefix(classNames);
    if (!globalClassFilter.isEmpty()) {
      this.globalClassFilter = globalClassFilter + "*";
    }
  }

  public void enableExceptionReporting(boolean notifyCaught, boolean notifyUncaught) {
    setEventRequestEnabled(eventRequestManager.createExceptionRequest(null, notifyCaught, notifyUncaught), true);
  }

  public Map<Integer, Boolean> createDebugClassFrom(ClassPrepareEvent event) throws ClassNotFoundException, AbsentInformationException {
    ReferenceType referenceType = event.referenceType();
    Class<?> clazz = Class.forName(referenceType.name());
    System.out.printf("Creating DebugClass instance for %s\n", clazz);
    DebugClassSource debugClassSource = debugClassSources.get(clazz);
    DebugClass debugClass = new DebugClass(clazz, debugClassSource, referenceType::locationsOfLine);
    debugClass.setCached(debugClassSource.isCached());
    Map<Integer, Boolean> addedBreakpoints = new HashMap<>(debugClassSource.getCompileTimeBreakpoints().size());
    for (int compileTimeBreakpoint : debugClassSource.getCompileTimeBreakpoints()) {
      boolean success = createBreakpointRequest(new DebugClassLocation(debugClass, compileTimeBreakpoint));
      addedBreakpoints.put(compileTimeBreakpoint, success);
    }
    debugClasses.put(clazz, debugClass);
    return addedBreakpoints;
  }

  public BreakpointRequest getBreakpointRequestAt(DebugClassLocation selectedLocation) {
    Class<?> clazz = selectedLocation.getDebugClass().getClazz();
    int lineNumber = selectedLocation.getLineNumber();
    return getDebugClassFor(clazz).getBreakpointRequest(lineNumber);
  }

  public boolean createBreakpointRequest(DebugClassLocation breakpointLocation) throws AbsentInformationException {
    DebugClass debugClass = breakpointLocation.getDebugClass();
    int lineNumber = breakpointLocation.getLineNumber();
    Location location = debugClass.getLocationOf(lineNumber);
    if (location != null) {
      BreakpointRequest breakpointRequest = eventRequestManager.createBreakpointRequest(location);
      setEventRequestEnabled(breakpointRequest, true);
      debugClass.addBreakpointRequest(lineNumber, breakpointRequest);
      return true;
    }
    return false;
  }

  public DebugClassLocation toDebugClassLocation(Location location) {
    DebugClassLocation debugClassLocation = null;
    String className = location.toString().split(":")[0];
    try {
      String[] classNameComponents = className.split("\\$");
      if (classNameComponents.length > 1) {
        className = classNameComponents[0];
      }
      Class<?> clazz = Class.forName(className);
      DebugClass debugClass = debugClasses.get(clazz);
      if (debugClass != null) {
        debugClassLocation = new DebugClassLocation(debugClass, location.lineNumber());
      }
    } catch (ClassNotFoundException ignored) {
    }
    return debugClassLocation;
  }

  public DebugClassSource getDebugClassSourceFor(Class<?> clazz) {
    return debugClassSources.get(clazz);
  }

  public DebugClass getDebugClassFor(Class<?> clazz) {
    return debugClasses.get(clazz);
  }

  public void setEventRequestEnabled(EventRequest eventRequest, boolean enabled) {
    if (!deadlockSafeInvoke) {
      eventRequest.setEnabled(enabled);
      eventRequestStateMap.put(eventRequest, enabled);
    }
  }

  public void deadlockSafeInvoke(Invokable toInvoke) throws Exception {
    deadlockSafeInvoke = true;
    for (Map.Entry<EventRequest, Boolean> eventRequestEntry : eventRequestStateMap.entrySet()) {
      eventRequestEntry.getKey().setEnabled(false);
    }
    toInvoke.invoke();
    for (Map.Entry<EventRequest, Boolean> eventRequestEntry : eventRequestStateMap.entrySet()) {
      eventRequestEntry.getKey().setEnabled(eventRequestEntry.getValue());
    }
    deadlockSafeInvoke = false;
  }

  public StepRequest createStepRequest(ThreadReference threadReference, Integer requestedStepRequestDepth) {
    StepRequest stepRequest = eventRequestManager.createStepRequest(threadReference, STEP_LINE, requestedStepRequestDepth);
    if (globalClassFilter != null) {
      stepRequest.addClassFilter(globalClassFilter);
    }
    setEventRequestEnabled(stepRequest, true);
    return stepRequest;
  }

}
