#!/bin/sh

javac ScaredyCat.java
javac MyBot1.java
./halite -d "240 160" "java MyBot1" "java ScaredyCat"