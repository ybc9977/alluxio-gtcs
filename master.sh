#!bin/bash

mkdir ~/testFile
touch ~/testFile/1.txt
echo "asodhfasiuhgilsjad" > ~/testFile/1.txt

for ((i=2; i<=49; i ++))
do
    cp ~/testFile/1.txt ~/testFile/$i.txt
done

~/alluxio-gtcs/bin/alluxio-start.sh master

for ((i=1; i<=49; i ++))
do
    ~/alluxio-gtcs/bin/alluxio fs copyFromLocal ~/testFile/$i.txt /
done
exit 0
