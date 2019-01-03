#!/bin/bash

# set $flintrockPemPath as an environmental variable

# $1 is the number of workers and $2 is the numnber of clients

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command gtcs "sudo rm -R alluxio-gtcs;git clone https://github.com/ybc9977/alluxio-gtcs.git"

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command gtcs "cd alluxio-gtcs; git pull; mvn clean install -DskipTests=true -Dlicense.skip=true -Dcheckstyle.skip -Dmaven.javadoc.skip=true"


exit 0

