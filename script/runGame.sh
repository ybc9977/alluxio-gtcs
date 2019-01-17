#!/usr/bin/env bash

# $1 is the number of runGame to be executed
# $2 is the file number


i=1
while test $i -ne $1
do
~/alluxio-gtcs/bin/alluxio runGame $2 300
let "i++"
done