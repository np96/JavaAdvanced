#!/usr/bin/env bash
pth=$(echo $1 | tr "[A-Z]" "[a-z]")
#cat src/ru/ifmo/ctddev/poperechnyi/$pth/$1.java
javac -cp "artifacts/"$1"Test.jar" src/ru/ifmo/ctddev/poperechnyi/$pth/*.java
java -cp "artifacts/"$1"Test.jar:lib/*:src" info.kgeorgiy.java.advanced.mapper.Tester $2 ru.ifmo.ctddev.poperechnyi.$pth."$1Impl",ru.ifmo.ctddev.poperechnyi.$pth.IterativeParallelism $3
#$1 — TaskName (with capital letters), $2 — task version , $3 — salt