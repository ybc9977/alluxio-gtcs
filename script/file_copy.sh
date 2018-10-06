#!/bin/bash

for ((i=1; i<=500; i ++)); 
do 
~/alluxio-gtcs/bin/alluxio fs copyFromLocal ~/testFile/$i.txt /test/$i.txt
done;