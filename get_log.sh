#!/usr/bin/env bash
read -r line < ~/flintrock.txt
ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "cd alluxio-gtcs; zip -r 1.zip ./logs" < /dev/null
scp -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem -r ${line}:~/alluxio-gtcs/1.zip ~/Desktop/Formal\ Data/

