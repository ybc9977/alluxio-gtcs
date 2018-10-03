#!/usr/bin/env bash


# stop all the master/client/worker
read -r line < ~/flintrock.txt

ssh -o StrictHostKeyChecking=no -i .ssh/gtcs.pem ${line} "~/alluxio-gtcs/bin/alluxio fs rm -R /test"

ssh -o StrictHostKeyChecking=no -i .ssh/gtcs.pem ${line} "~/alluxio-gtcs/bin/alluxio-stop.sh all"

python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "~/alluxio-gtcs/bin/alluxio-stop.sh local"

python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "rm -rf ~/alluxio-gtcs/logs;rm -rf ~/alluxio-gtcs/journal;rm ~/alluxio-gtcs/conf/workers"
