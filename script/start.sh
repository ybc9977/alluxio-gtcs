#!/bin/bash

# set $flintrockPemPath as an environmental variable

# start

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command gtcs "touch ~/alluxio-gtcs/conf/workers"

i=1
while read -r line
do
    test $i -lt 2 && let "i++" && continue
    test $i -gt $[1+$1] && let "i++" && continue
    python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command gtcs "echo '${line:9}' >> ~/alluxio-gtcs/conf/workers"
    let "i++"
done < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt


read -r line < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt
ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${line} " sh ~/Hadoop/sbin/start-dfs.sh;~/alluxio-gtcs/bin/alluxio format;~/alluxio-gtcs/bin/alluxio-start.sh all SudoMount"

# python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command gtcs "chmod -R 770 /mnt/"

# i=1
# while read -r line
# do
#     test $i -le 1 && let "i++" && continue
#     test $i -gt $[1+$1] && let "i++" && continue
#     ssh -o StrictHostKeyChecking=no -i .ssh/gtcs.pem ${line} "~/alluxio-gtcs/bin/alluxio-start.sh worker SudoMount"
#     let "i++"
# done < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt

i=1
while read -r line
do
    test $i -le $[1+$1] && let "i++" && continue
    ssh -o StrictHostKeyChecking=no -i $flintrockPemPath  ${line} "~/alluxio-gtcs/bin/alluxio-start.sh client" < /dev/null
    let "i++"
done < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt

sleep 10

read -r line < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt
ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${line} "sh ~/alluxio-gtcs/script/file_copy.sh"

exit 0
