package com.swilkins.scrabbleapi.executable;

import com.swilkins.ScrabbleBase.Board.Configuration;
import com.swilkins.ScrabbleBase.Board.State.BoardSquare;
import com.swilkins.ScrabbleBase.Board.State.Rack;
import com.swilkins.ScrabbleBase.Generation.Generator;
import com.swilkins.ScrabbleBase.Generation.GeneratorResult;
import com.swilkins.ScrabbleBase.Vocabulary.PermutationTrie;

import java.net.URL;

public class GeneratorTarget {

  public static void main(String[] args) {
    Rack rack = new Rack(Configuration.STANDARD_RACK_CAPACITY);
    rack.addAllFromLetters("ab*e");
    BoardSquare[][] board = Configuration.getStandardBoard();
    board[7][7].setTile(Configuration.getStandardTile('f'));
    board[7][8].setTile(Configuration.getStandardTile('i'));
    board[7][9].setTile(Configuration.getStandardTile('s'));
    board[7][10].setTile(Configuration.getStandardTile('h'));
    PermutationTrie trie = new PermutationTrie();
    URL dictionary = GeneratorTarget.class.getResource("../resource/ospd4.txt");
    trie.loadFrom(dictionary, String::trim);
    Generator generator = new Generator(trie, Configuration.STANDARD_RACK_CAPACITY);
    GeneratorResult result = generator.compute(rack, board).orderBy(Generator.getDefaultOrdering());
    System.out.println(result.get(0));
  }

}
