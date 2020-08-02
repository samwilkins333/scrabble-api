package com.swilkins.scrabbleapi.controller;

import com.google.common.collect.Lists;
import com.swilkins.ScrabbleBase.Board.State.BoardSquare;
import com.swilkins.ScrabbleBase.Board.State.Rack;
import com.swilkins.ScrabbleBase.Board.State.Tile;
import com.swilkins.ScrabbleBase.Generation.Candidate;
import com.swilkins.ScrabbleBase.Generation.Generator;
import com.swilkins.ScrabbleBase.Vocabulary.PermutationTrie;
import com.swilkins.scrabbleapi.model.BoardRow;
import com.swilkins.scrabbleapi.model.GenerationContext;
import com.swilkins.scrabbleapi.model.GenerationResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.swilkins.ScrabbleBase.Board.Configuration.*;
import static com.swilkins.ScrabbleBase.Generation.Generator.getDefaultOrdering;

@RestController
public class GenerationController {
  private static PermutationTrie trie;

  static {
    trie = new PermutationTrie();
    URL dictionary = GenerationController.class.getResource("/static/ospd4.txt");
    trie.loadFrom(dictionary, String::trim);
  }

  @PostMapping("/generate")
  public GenerationResponse generate(@NotNull @RequestBody GenerationContext context) {
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
    response.pageCount = 1;

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
    List<Candidate> candidates = new Generator(trie, STANDARD_RACK_CAPACITY).compute(rack, board, getDefaultOrdering());
    response.pageSize = candidates.size();

    if (options != null && options.raw) {
      output.addAll(candidates);
    } else {
      output.addAll(candidates.stream().map(Candidate::toString).collect(Collectors.toList()));
    }
    if (options != null && options.pageSize != null) {
      if (options.pageSize <= 0) {
        return null;
      }
      if (options.pageSize < output.size()) {
        output = new ArrayList<>(Lists.partition(output, options.pageSize));
        response.pageSize = options.pageSize;
        response.pageCount = output.size();
      }
    }
    response.serializedBoard = serializeBoard(board);
    response.count = candidates.size();
    response.candidates = output;

    return response;
  }

}
