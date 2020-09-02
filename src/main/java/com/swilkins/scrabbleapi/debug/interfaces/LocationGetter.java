package com.swilkins.scrabbleapi.debug.interfaces;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;

import java.util.List;

public interface LocationGetter {

  List<Location> getLocations(int lineNumber) throws AbsentInformationException;

}
