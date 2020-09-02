package com.swilkins.scrabbleapi.debug.interfaces;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DebugClassSource {

  boolean main() default false;

  String[] args() default {};

  int[] compileTimeBreakpoints() default {};

  boolean cached() default false;

}
