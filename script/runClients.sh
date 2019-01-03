#!/bin/bash

#$1 worker number

# set $flintrockPemPath as an environmental variable

# kill the clients first
sh $(cd `dirname $0`; pwd)/killClients.sh $1

i=1
while read -r line
do
    test $i -le $[1+$1] && let "i++" && continue
    ssh -o StrictHostKeyChecking=no -i $flintrockPemPath  ${line} "~/alluxio-gtcs/bin/alluxio-start.sh client" < /dev/null
    let "i++"
done < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt

# sleep 10
value1="After add: "
value2=" users."
value=${value1}${2}${value2}
read -r line < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt


#until ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${line} 'grep -q "'${value}'" ~/alluxio-gtcs/logs/master.out' < /dev/null
#do
 #  continue
#done
#ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${line} "~/alluxio-gtcs/bin/alluxio runGame 200 120"


exit 0
