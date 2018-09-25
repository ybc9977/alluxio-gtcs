#!bin/bash

mkdir ~/testFile
touch ~/testFile/1.txt
echo "asodhfasiuhgilsjad" > ~/testFile/1.txt

for ((i=2; i<10; i ++))
do
    cp ~/testFile/1.txt ~/testFile/$i.txt
done

~/alluxio-gtcs/bin/alluxio-start.sh master

for ((i=1; i<=10; i ++))
do
    ~/alluxio-gtcs/bin/alluxio fs copyFromLocal ~/testFile/$i.txt /
    sleep 1
done
exit 0
