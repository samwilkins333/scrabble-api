package com.swilkins.scrabbleapi.controller.generation.model;

import java.util.List;

public class GenerationContext {

  public List<BoardRow> board;
  public String rack;
  public Options options;

  public static class Options {
    public boolean raw;
    public Integer pageSize;
  }

}
