#!/usr/bin/env bash

i=1
while test $i -ne $1
do
~/alluxio-gtcs/bin/alluxio runGame 500 300
let "i++"
done