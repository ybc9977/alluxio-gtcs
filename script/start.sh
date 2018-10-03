#!/bin/bash

# start

i=1
while read -r line
do
    test $i -lt 2 && let "i++" && continue
    test $i -gt 1+$1 && let "i++" && continue
    python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "echo '${line}' >> ~/alluxio-gtcs/conf/workers"
    let "i++"
done < ~/flintrock.txt

read -r line < ~/flintrock.txt
ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "~/alluxio-gtcs/bin/alluxio-start.sh all"

i=1
while read -r line
do
    test $i -le 1+$1 && let "i++" && continue
    ssh -o StrictHostKeyChecking=no -i .ssh/gtcs.pem ${line} "~/alluxio-gtcs/bin/alluxio-start.sh client"
    let "i++"
done < ~/flintrock.txt

sleep 30

read -r line < ~/flintrock.txt
ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "for ((i=1; i<=500; i ++)); do ~/alluxio-gtcs/bin/alluxio fs copyFromLocal ~/testFile/\$i.txt /test; done;"

exit 0
