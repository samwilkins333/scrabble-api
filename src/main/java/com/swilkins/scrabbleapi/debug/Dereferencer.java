package com.swilkins.scrabbleapi.debug;

import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;

public interface Dereferencer {

  Object dereference(ObjectReference object, ThreadReference thread) throws NoSuchMethodException;

}
