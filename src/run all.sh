#!/bin/sh

javac MyBot.java
javac MyBot1.java
javac MyBotOld.java
javac MyBot3.java
./halite -d "240 160" "java MyBot" "java MyBot3" "java MyBot1" "java MyBotOld"