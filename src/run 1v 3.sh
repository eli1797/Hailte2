#!/bin/sh

javac MyBot1.java
javac MyBot3.java
./halite -d "240 160" "java MyBot3" "java MyBot1"