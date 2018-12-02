#!/usr/bin/env bash

mkdir ~/Desktop/gtcs_log/

read -r line < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt

# ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "cd alluxio-gtcs; zip -r master.zip ./logs" < /dev/null

scp -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem -r ${line}:~/alluxio-gtcs/python/prefs.txt ~/Desktop/gtcs_log/

# scp -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem -r ${line}:~/alluxio-gtcs/master.txt ~/Desktop/gtcs_log/

# ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "cd alluxio-gtcs; zip -r 2.zip ./conf" < /dev/null

# scp -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem -r ${line}:~/alluxio-gtcs/2.zip ~/Desktop/

i=1
while read -r line
do
    let "i++"

    scp -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem -r ${line}:~/*.txt ~/Desktop/gtcs_log/

done < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt

