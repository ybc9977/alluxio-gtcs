#!/usr/bin/env bash

mkdir testFile

cd ~/testFile

dd if=/dev/zero of=1.txt bs=10485760 count=1

for ((i=2; i<=500; i ++))
do
    cp ~/testFile/1.txt ~/testFile/$i.txt
done