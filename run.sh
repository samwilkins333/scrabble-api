#!/usr/bin/env bash

cd src/main/java
javac -g -cp ".:/Users/swilkinss2012/.m2/repository/org/springframework/boot/spring-boot-autoconfigure/2.3.2.RELEASE/spring-boot-autoconfigure-2.3.2.RELEASE.jar" com/swilkins/scrabbleapi/ScrabbleApiApplication.java;
java com/swilkins/scrabbleapi/ScrabbleApiApplication 10;
