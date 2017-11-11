#!/bin/sh

javac MyBot.java
javac MyBot2.java
./halite -d "240 160" "java MyBot2" "java MyBot"