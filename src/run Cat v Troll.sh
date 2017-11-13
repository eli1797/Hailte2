#!/bin/sh

javac ScaredyCat.java
javac Troll.java
./halite -d "240 160" "java Troll" "java ScaredyCat"