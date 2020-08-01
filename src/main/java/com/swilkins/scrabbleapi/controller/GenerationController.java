package com.swilkins.scrabbleapi.controller;

import com.swilkins.scrabbleapi.model.GenerationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
public class GenerationController {

  @PostMapping("/generate")
  public void generate(@RequestBody GenerationContext context){
    System.out.println(Arrays.deepToString(context.board));
    System.out.println(Arrays.toString(context.rack));
  }

}
