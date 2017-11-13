#!/bin/sh

javac ScaredyCat.java
javac MyBot3.java
./halite -d "240 160" "java MyBot3" "java ScaredyCat"