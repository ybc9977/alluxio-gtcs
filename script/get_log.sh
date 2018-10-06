#!/usr/bin/env bash

read -r line < ~/flintrock.txt

ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "cd alluxio-gtcs; zip -r master.zip ./logs" < /dev/null

scp -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem -r ${line}:~/alluxio-gtcs/master.zip ~/Desktop/Data/

# ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "cd alluxio-gtcs; zip -r 2.zip ./conf" < /dev/null

# scp -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem -r ${line}:~/alluxio-gtcs/2.zip ~/Desktop/

# i=1
# while read -r line
# do
#     test $i -ne 2 && let "i++" && continue

#     ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "cd alluxio-gtcs; zip -r 3.zip ./logs" < /dev/null
#     scp -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem -r ${line}:~/alluxio-gtcs/3.zip ~/Desktop/

# done < ~/flintrock.txt

