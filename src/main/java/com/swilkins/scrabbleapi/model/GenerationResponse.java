package com.swilkins.scrabbleapi.model;

import java.util.List;

public class GenerationResponse {

  public GenerationContext context;
  public List<String> serializedBoard;
  public int count;
  public int pageSize;
  public int pageCount;
  public List<Object> candidates;

}
