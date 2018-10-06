#!/bin/bash

# start

i=1
while read -r line
do
    test $i -lt 2 && let "i++" && continue
    test $i -gt $[1+$1] && let "i++" && continue
    python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "echo '${line:9}' >> ~/alluxio-gtcs/conf/workers"
    let "i++"
done < ~/flintrock.txt


read -r line < ~/flintrock.txt
ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} " ~/Hadoop/sbin/start-dfs;~/alluxio-gtcs/bin/alluxio-start.sh all SudoMount"

# python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "chmod -R 770 /mnt/"

# i=1
# while read -r line
# do
#     test $i -le 1 && let "i++" && continue
#     test $i -gt $[1+$1] && let "i++" && continue
#     ssh -o StrictHostKeyChecking=no -i .ssh/gtcs.pem ${line} "~/alluxio-gtcs/bin/alluxio-start.sh worker SudoMount"
#     let "i++"
# done < ~/flintrock.txt

i=1
while read -r line
do
    test $i -le $[1+$1] && let "i++" && continue
    ssh -o StrictHostKeyChecking=no -i .ssh/gtcs.pem ${line} "~/alluxio-gtcs/bin/alluxio-start.sh client"
    let "i++"
done < ~/flintrock.txt

sleep 30

read -r line < ~/flintrock.txt
ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "sh ~/alluxio-gtcs/script/file_copy.sh"

exit 0
