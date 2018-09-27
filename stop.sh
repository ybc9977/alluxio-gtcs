#!/usr/bin/env bash


# stop all the master/client/worker
python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "~/alluxio-gtcs/bin/alluxio-stop.sh all"

# clear logs/UnderFileStorage
python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "rm ~/alluxio-gtcs/logs;rm ~/alluxio-gtcs/underFSStorage/*.txt"

