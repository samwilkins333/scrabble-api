package com.swilkins.scrabbleapi.controller;

import com.swilkins.ScrabbleBase.Vocabulary.PermutationTrie;
import com.swilkins.scrabbleapi.model.BoardRow;
import com.swilkins.scrabbleapi.model.GenerationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.util.List;
import java.util.Map;

import static com.swilkins.ScrabbleBase.Board.Configuration.STANDARD_BOARD_DIMENSIONS;
import static com.swilkins.scrabbleapi.ScrabbleApiApplication.dereferencedVariables;

@RestController
public class GenerationController {
  private static final PermutationTrie trie = new PermutationTrie();

  static {
    URL dictionary = GenerationController.class.getResource("/ospd4.txt");
    trie.loadFrom(dictionary, String::trim);
  }

  @PostMapping("/api/generate")
  public Map<String, Object> generate(@RequestBody GenerationContext context) {
    List<BoardRow> boardSource = context.board;
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

    return dereferencedVariables;
  }

}
