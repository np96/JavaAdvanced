#!/usr/bin/env bash
dir=`mktemp -d`
wdir=`pwd`
javac -d $dir -cp "$wdir/artifacts/ImplementorTest.jar" $wdir/src/ru/ifmo/ctddev/poperechnyi/implementor/*.java
cd $dir
cp -v $wdir/artifacts/ImplementorTest.jar .
jar vcmf $wdir/Manifest.txt $wdir/ImplJar.jar .
cd $wdir
rm -rf $dir