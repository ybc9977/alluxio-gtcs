#!/bin/bash

#$1 worker number

# set $flintrockPemPath as an environmental variable


i=1
while read -r line
do
    test $i -le $[1+$1] && let "i++" && continue
    ssh -o StrictHostKeyChecking=no -i $flintrockPemPath  ${line} "ps ax | grep AlluxioGameSystemServer |awk -F ' ' '{print \$1}' | xargs kill -9" < /dev/null
    let "i++"
done < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt

exit 0
