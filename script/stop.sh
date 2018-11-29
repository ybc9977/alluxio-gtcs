#!/usr/bin/env bash


# stop all the master/client/worker
read -r line < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt

ssh -o StrictHostKeyChecking=no -i .ssh/gtcs.pem ${line} "~/alluxio-gtcs/bin/alluxio fs rm -R /test"

ssh -o StrictHostKeyChecking=no -i .ssh/gtcs.pem ${line} "~/alluxio-gtcs/bin/alluxio-stop.sh all"

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command gtcs "~/alluxio-gtcs/bin/alluxio-stop.sh local"

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command gtcs "rm -rf ~/alluxio-gtcs/logs;rm -rf ~/alluxio-gtcs/journal;rm ~/alluxio-gtcs/conf/workers"
