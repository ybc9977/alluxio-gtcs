#!/bin/bash

python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "~/alluxio-gtcs/bin/alluxio-start.sh worker SudoMount; ~/alluxio-gtcs/bin/alluxio-start.sh client"

read -r line < ~/flintrock.txt

ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "~/alluxio-gtcs/bin/alluxio-start.sh master"

ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "for ((i=1; i<= $1; i ++)); do ~/alluxio-gtcs/bin/alluxio fs copyFromLocal ~/testFile/\$i.txt /; done;"

exit 0
