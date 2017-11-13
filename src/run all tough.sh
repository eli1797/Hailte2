#!/bin/sh

javac ScaredyCat.java
javac Sloth.java
javac Troll.java
javac MyBot3.java
./halite -d "240 160" "java Sloth" "java MyBot3" "java Troll" "java ScaredyCat"