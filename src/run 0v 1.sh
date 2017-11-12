#!/bin/sh

javac MyBot.java
javac MyBot1.java
./halite -d "240 160" "java MyBot1" "java MyBot"