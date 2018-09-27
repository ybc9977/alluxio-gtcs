#!/bin/bash
read -r line < ~/flintrock.txt

ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "for ((i=1; i<= $1; i ++)); do ~/alluxio-gtcs/bin/alluxio fs copyFromLocal ~/testFile/\$i.txt /; done;"

exit 0
