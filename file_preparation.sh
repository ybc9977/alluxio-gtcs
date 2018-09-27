#!/usr/bin/env bash

mkdir ~/testFile
touch ~/testFile/1.txt
echo "asodhfasiuhgilsjad" > ~/testFile/1.txt

for ((i=2; i<=120; i ++))
do
    cp ~/testFile/1.txt ~/testFile/$i.txt
done