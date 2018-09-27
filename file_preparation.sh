#!/usr/bin/env bash

mkdir ~/testFile
touch ~/testFile/1.txt
echo "asodhfasiuhgilsjad" > ~/testFile/1.txt

for ((i=2; i<=100; i ++))
do
    cp ~/testFile/1.txt ~/testFile/$i.txt
done

~/alluxio-gtcs/bin/alluxio-start.sh master