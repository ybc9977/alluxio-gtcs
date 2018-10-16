#!/bin/bash

for ((i=51; i<=500; i ++));
do 
~/alluxio-gtcs/bin/alluxio fs copyFromLocal ~/testFile/1.txt /test/$i.txt
done;