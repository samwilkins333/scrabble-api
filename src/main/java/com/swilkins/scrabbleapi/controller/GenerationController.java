package com.swilkins.scrabbleapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swilkins.ScrabbleBase.Board.State.BoardSquare;
import com.swilkins.ScrabbleBase.Board.State.Rack;
import com.swilkins.ScrabbleBase.Board.State.Tile;
import com.swilkins.ScrabbleBase.Generation.Generator;
import com.swilkins.ScrabbleBase.Generation.GeneratorResult;
import com.swilkins.ScrabbleBase.Vocabulary.PermutationTrie;
import com.swilkins.scrabbleapi.model.BoardRow;
import com.swilkins.scrabbleapi.model.GenerationContext;
import com.swilkins.scrabbleapi.model.GenerationResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.swilkins.ScrabbleBase.Board.Configuration.*;
import static com.swilkins.ScrabbleBase.Generation.Generator.getDefaultOrdering;
import static com.swilkins.scrabbleapi.FibonacciDebugger.dereferencedVariables;

@RestController
public class GenerationController {
  private static final PermutationTrie trie = new PermutationTrie();

  static {
    URL dictionary = GenerationController.class.getResource("/ospd4.txt");
    trie.loadFrom(dictionary, String::trim);
  }

  @PostMapping("/api/generate")
  public String generate(@RequestBody GenerationContext context) throws JsonProcessingException {
    List<BoardRow> boardSource = context.board;
    GenerationContext.Options options = context.options;
    if (boardSource.size() > STANDARD_BOARD_DIMENSIONS) {
      return null;
    }
    synchronized (dereferencedVariables) {
      System.out.printf("Counter before waiting = %s\n", dereferencedVariables.get("status"));
      dereferencedVariables.notifyAll();
      try {
        dereferencedVariables.wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      System.out.printf("Counter after waiting = %s\n", dereferencedVariables.get("status"));
    }
    Set<Integer> encounteredRows = new HashSet<>();
    for (BoardRow boardRow : boardSource) {
      int n = boardRow.row;
      if (n < 0 || n >= STANDARD_BOARD_DIMENSIONS || encounteredRows.contains(n) ||
              !boardRow.tiles.matches(String.format("[a-zA-Z\\-]{%d}", STANDARD_BOARD_DIMENSIONS))) {
        return null;
      }
      encounteredRows.add(n);
    }
    String rackSource = context.rack;
    if (rackSource.isEmpty() || rackSource.length() > STANDARD_RACK_CAPACITY ||
            !rackSource.matches(String.format("[a-z%c]+", Tile.BLANK))) {
      return null;
    }

    GenerationResponse response = new GenerationResponse();
    response.context = context;

    BoardSquare[][] board = getStandardBoard();
    for (BoardRow inputRow : boardSource) {
      BoardSquare[] outputRow = board[inputRow.row];
      for (int x = 0; x < STANDARD_BOARD_DIMENSIONS; x++) {
        char l = inputRow.tiles.charAt(x);
        if (Character.isLetter(l)) {
          Tile tile;
          if (Character.isUpperCase(l)) {
            tile = getStandardTile(Tile.BLANK);
            tile.setLetterProxy(Character.toLowerCase(l));
          } else {
            tile = getStandardTile(l);
          }
          outputRow[x].setTile(tile);
        }
      }
    }

    Rack rack = new Rack(STANDARD_RACK_CAPACITY);
    rack.addAllFromLetters(rackSource);

    List<Object> output = new ArrayList<>();
    GeneratorResult result = new Generator(trie, STANDARD_RACK_CAPACITY).compute(rack, board);
    result.orderBy(getDefaultOrdering());
    response.pageSize = result.size();

    Integer resolvedPageSize =
            options != null &&
                    options.pageSize != null &&
                    options.pageSize > 0 &&
                    options.pageSize < result.size() ? options.pageSize : null;
    response.pageSize = resolvedPageSize != null ? resolvedPageSize : result.size();
    if (options != null && options.raw) {
      output.addAll(result.asNewPlacementsList(resolvedPageSize));
    } else {
      output.addAll(result.asSerializedList(resolvedPageSize));
    }
    response.pageCount = resolvedPageSize != null ? output.size() : 1;
    response.serializedBoard = serializeBoard(board);
    response.count = result.size();
    response.candidates = output;

    return new ObjectMapper().writeValueAsString(response);
  }

}
