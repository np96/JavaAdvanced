#!/usr/bin/env bash
rm -rf implDoc/*
gen=$(find src/ru/ifmo/ctddev/poperechnyi/implementor/ -iname "*.java")
kgeorgiy=$(find src/info/kgeorgiy/java/advanced/implementor/Impler.java src/info/kgeorgiy/java/advanced/implementor/ImplerException.java src/info/kgeorgiy/java/advanced/implementor/JarImpler.java)
mkdir implDoc
javadoc -d implDoc -linkoffline http://docs.oracle.com/javase/7/docs/api/ http://docs.oracle.com/javase/7/docs/api/ -d doc -private $gen $kgeorgiy