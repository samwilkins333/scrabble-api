package com.swilkins.scrabbleapi.debuggers.generator;

import com.sun.jdi.ObjectReference;
import com.swilkins.ScrabbleBase.Board.Location.Coordinates;
import com.swilkins.ScrabbleBase.Board.Location.TilePlacement;
import com.swilkins.ScrabbleBase.Board.State.BoardSquare;
import com.swilkins.ScrabbleBase.Board.State.Tile;
import com.swilkins.ScrabbleBase.Generation.Candidate;
import com.swilkins.ScrabbleBase.Generation.CrossedTilePlacement;
import com.swilkins.ScrabbleBase.Generation.Direction;
import com.swilkins.scrabbleapi.debug.Debugger;
import com.swilkins.scrabbleapi.debug.Dereferencer;

import java.io.IOException;

public class GeneratorDebugger extends Debugger {
  private static final String jarPathCore = "/lib/scrabble-base-0.0.1.jar";

  public GeneratorDebugger() throws IllegalArgumentException, IOException, ClassNotFoundException {
    super();
  }

  @Override
  protected void configureDebuggerModel() throws IOException, ClassNotFoundException {
    super.configureDebuggerModel();
    importJar(getClass().getResource(jarPathCore).getPath(), null, true);
  }

  @Override
  protected void configureDereferencers() {
    super.configureDereferencers();
    Dereferencer fromTileContainer = (tileWrapper, thread) -> standardDereference(tileWrapper, "getTile", thread);
    dereferencerMap.put(BoardSquare.class, fromTileContainer);
    dereferencerMap.put(TilePlacement.class, (tilePlacement, thread) -> new Object[]{
            standardDereference(tilePlacement, "getX", thread),
            standardDereference(tilePlacement, "getY", thread),
            fromTileContainer.dereference(tilePlacement, thread)
    });
    dereferencerMap.put(Tile.class, (tile, thread) -> new Object[]{
            standardDereference(tile, "getLetter", thread),
            standardDereference(tile, "getLetterProxy", thread)
    });
    dereferencerMap.put(Direction.class, (direction, thread) -> {
      ObjectReference directionNameReference = (ObjectReference) invoke(direction, thread, "name", null, null);
      return toString.dereference(directionNameReference, thread);
    });
    dereferencerMap.put(Character.class, (character, thread) -> standardDereference(character, "charValue", thread));
    dereferencerMap.put(Candidate.class, (candidate, thread) -> new Object[]{
            standardDereference(candidate, "getScore", thread),
            toString.dereference(candidate, thread)
    });
    dereferencerMap.put(Coordinates.class, (coordinates, thread) -> new Object[]{
            standardDereference(coordinates, "getX", thread),
            standardDereference(coordinates, "getY", thread)
    });
    dereferencerMap.put(CrossedTilePlacement.class, (crossedTilePlacement, thread) -> standardDereference(crossedTilePlacement, "getRoot", thread));
  }

  @Override
  protected void onVirtualMachineSuspension() {
    System.out.println(dereferencedVariables);
  }

}
