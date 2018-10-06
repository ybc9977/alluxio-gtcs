#!/bin/bash

# $1 is the number of workers and $2 is the numnber of clients


#launch
python3 ~/Downloads/flintrock-master/standalone.py launch gtcs

#set up
python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "sudo yum update -y"
python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "sudo yum install java-1.8.0-openjdk* -y"
python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs 'export JAVA_HOME="/usr/lib/jvm/java-1.8.0-openjdk"'

python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "sudo wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo"
python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs 'sudo sed -i s/\$releasever/7/g /etc/yum.repos.d/epel-apache-maven.repo'
python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "sudo yum -y install apache-maven"

python3 ~/Downloads/flintrock-master/standalone.py describe gtcs

python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "sudo yum -y install git"
python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "git clone https://github.com/ybc9977/alluxio-gtcs.git"

# python3 ~/Downloads/flintrock-master/standalone.py copy-file gtcs /Users/ybc/Downloads/alluxio-gtcs.zip /home/ec2-user/
# python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "unzip alluxio-gtcs"

# python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "rm alluxio-gtcs/core/server/master/target/miredot/font/Droid_Sans/LICENSE.txt"
# python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "rm alluxio-gtcs/core/server/master/target/miredot/font/Droid_Sans_Mono/LICENSE.txt"
# python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "rm alluxio-gtcs/core/server/master/target/miredot/font/Open_Sans/LICENSE.txt"
# python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "rm alluxio-gtcs/core/server/proxy/target/miredot/font/Droid_Sans/LICENSE.txt"
# python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "rm alluxio-gtcs/core/server/proxy/target/miredot/font/Droid_Sans_Mono/LICENSE.txt"
# python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "rm alluxio-gtcs/core/server/proxy/target/miredot/font/Open_Sans/LICENSE.txt"
# python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "rm alluxio-gtcs/core/server/worker/target/miredot/font/Droid_Sans/LICENSE.txt"
# python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "rm alluxio-gtcs/core/server/worker/target/miredot/font/Droid_Sans_Mono/LICENSE.txt"
# python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "rm alluxio-gtcs/core/server/worker/target/miredot/font/Open_Sans/LICENSE.txt"

python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "cd alluxio-gtcs;mvn clean install -DskipTests=true -Dlicense.skip=true -Dcheckstyle.skip -Dmaven.javadoc.skip=true"

read -r line < ~/flintrock.txt
python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "cp ~/alluxio-gtcs/conf/alluxio-site.properties.template ~/alluxio-gtcs/conf/alluxio-site.properties"
python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "cp ~/alluxio-gtcs/conf/alluxio-env.sh.template ~/alluxio-gtcs/conf/alluxio-env.sh"
python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "echo 'alluxio.master.hostname=${line:9}' >> ~/alluxio-gtcs/conf/alluxio-site.properties"
python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "echo 'alluxio.underfs.address=hdfs://${line:9}:9000'>> ~/alluxio-gtcs/conf/alluxio-site.properties"
python3 ~/Downloads/flintrock-master/standalone.py run-command gtcs "echo ${line:9} >> ~/alluxio-gtcs/conf/masters"

read -r line < ~/flintrock.txt
ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "sh ~/alluxio-gtcs/script/file_preparation.sh"

sh ~/Github/alluxio-gtcs/script/start.sh $1

exit 0