package com.swilkins.scrabbleapi.controller;

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

@RestController
public class GenerationController {
  private static PermutationTrie trie;

  static {
    trie = new PermutationTrie();
    URL dictionary = GenerationController.class.getResource("/ospd4.txt");
    trie.loadFrom(dictionary, String::trim);
  }

  @PostMapping("/api/generate")
  public GenerationResponse generate(@RequestBody GenerationContext context) {
    List<BoardRow> boardSource = context.board;
    GenerationContext.Options options = context.options;
    if (boardSource.size() > STANDARD_BOARD_DIMENSIONS) {
      return null;
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

    boolean paginated =
            options != null &&
                    options.pageSize != null &&
                    options.pageSize > 0 &&
                    options.pageSize < result.size();
    response.pageSize = paginated ? options.pageSize : result.size();
    if (options != null && options.raw) {
      output.addAll(result.asNewPlacements());
    } else {
      output.addAll(paginated ? result.asPagedSerializedList(options.pageSize) : result.asFlatSerializedList());
    }
    response.pageCount = paginated ? output.size() : 1;
    response.serializedBoard = serializeBoard(board);
    response.count = result.size();
    response.candidates = output;

    return response;
  }

}
