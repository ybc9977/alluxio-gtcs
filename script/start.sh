#!/bin/bash

# set $flintrockPemPath as an environmental variable

# $1: worker number

# clear existing logs
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command gtcs "rm  ~/alluxio-gtcs/*.txt"


python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py describe gtcs

# start
read -r line < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command gtcs "cp ~/alluxio-gtcs/conf/alluxio-site.properties.template ~/alluxio-gtcs/conf/alluxio-site.properties"
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command gtcs "cp ~/alluxio-gtcs/conf/alluxio-env.sh.template ~/alluxio-gtcs/conf/alluxio-env.sh"
#python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command gtcs "echo   'alluxio.worker.memory.size=32GB' >> ~/alluxio-gtcs/conf/alluxio-site.properties"
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command gtcs "echo 'alluxio.master.hostname=${line:9}' >> ~/alluxio-gtcs/conf/alluxio-site.properties"
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command gtcs "echo 'alluxio.underfs.address=hdfs://${line:9}:9000'>> ~/alluxio-gtcs/conf/alluxio-site.properties"
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command gtcs "echo ${line:9} > ~/alluxio-gtcs/conf/masters"








python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command gtcs "rm  ~/alluxio-gtcs/conf/workers;touch ~/alluxio-gtcs/conf/workers"

i=1
while read -r line
do
    test $i -lt 2 && let "i++" && continue
    test $i -gt $[1+$1] && let "i++" && continue
    python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command gtcs "echo '${line:9}' >> ~/alluxio-gtcs/conf/workers"
    let "i++"
done < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt


read -r line < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt
ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${line} " sh ~/hadoop/sbin/start-dfs.sh;~/alluxio-gtcs/bin/alluxio format;~/alluxio-gtcs/bin/alluxio-start.sh all SudoMount"

#python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command gtcs "chmod -R 770 /mnt/"

# i=1
# while read -r line
# do
#     test $i -le 1 && let "i++" && continue
#     test $i -gt $[1+$1] && let "i++" && continue
#     ssh -o StrictHostKeyChecking=no -i .ssh/gtcs.pem ${line} "~/alluxio-gtcs/bin/alluxio-start.sh worker SudoMount"
#     let "i++"
# done < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt



exit 0
