#!/usr/bin/env bash


# stop all the master/client/worker
read -r line < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt

ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${line} "~/alluxio-gtcs/bin/alluxio fs rm -R /test"

ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${line} "~/alluxio-gtcs/bin/alluxio-stop.sh all"

# ssh -o StrictHostKeyChecking=no -i .ssh/gtcs.pem ${line} "rm *.txt"

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command gtcs "~/alluxio-gtcs/bin/alluxio-stop.sh local"

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command gtcs "rm -rf ~/alluxio-gtcs/logs;rm -rf ~/alluxio-gtcs/journal;rm ~/alluxio-gtcs/conf/workers;rm ~/alluxio-gtcs/*.txt;rm ~/alluxio-gtcs/python/*.txt;rm ~/*.txt"

python3 flintrock/standalone.py stop gtcs
