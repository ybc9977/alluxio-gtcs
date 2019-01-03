#!/usr/bin/env bash

# set $flintrockPemPath as an environmental variable
   
# $1: worker number

mkdir ~/Desktop/gtcs_log/

read -r line < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt

# ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "cd alluxio-gtcs; zip -r master.zip ./logs" < /dev/null

scp -o StrictHostKeyChecking=no -i $flintrockPemPath -r ${line}:~/alluxio-gtcs/python/*.txt ~/Desktop/gtcs_log/

scp -o StrictHostKeyChecking=no -i $flintrockPemPath -r ${line}:~/alluxio-gtcs/logs/*.log ~/Desktop/gtcs_log/

scp -o StrictHostKeyChecking=no -i $flintrockPemPath -r ${line}:~/alluxio-gtcs/logs/*.out ~/Desktop/gtcs_log/

scp -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem -r ${line}:~/alluxio-gtcs/master.txt ~/Desktop/gtcs_log/

# ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "cd alluxio-gtcs; zip -r 2.zip ./conf" < /dev/null

# scp -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem -r ${line}:~/alluxio-gtcs/2.zip ~/Desktop/

i=1
while read -r line
do
    let "i++"

    scp -o StrictHostKeyChecking=no -i $flintrockPemPath -r ${line}:~/*.txt ~/Desktop/gtcs_log/

done < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt

