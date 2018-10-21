#!/bin/bash

for ((i=1; i<=30; i ++));
do 
~/alluxio-gtcs/bin/alluxio fs copyFromLocal ~/testFile/1.txt /test/$i.txt
done;