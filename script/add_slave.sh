#!/bin/bash

# $1 is the number of workers while $2 is the number of clients

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py/standalone.py add-slaves gtcs --num-slaves $1

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py/standalone.py describe gtcs

read -r k < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt

i=1
while read -r line
do
    test $i -le $2 && let "i++" && continue

    ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "sudo yum update -y;sudo yum install java-1.8.0-openjdk* -y;" < /dev/null

    ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} 'export JAVA_HOME="/usr/lib/jvm/java-1.8.0-openjdk"' < /dev/null

    ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "sudo wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo" < /dev/null

    ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} 'sudo sed -i s/\$releasever/7/g /etc/yum.repos.d/epel-apache-maven.repo' < /dev/null
    
    ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "sudo yum -y install python2-pip;sudo yum -y install gcc-c++;sudo pip install numpy==1.14.4;sudo pip install cvxpy==0.4.9" < /dev/null

    ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "sudo yum -y install apache-maven;sudo yum -y install git;git clone https://github.com/ybc9977/alluxio-gtcs.git;cd alluxio-gtcs;mvn clean install -DskipTests=true -Dlicense.skip=true -Dcheckstyle.skip -Dmaven.javadoc.skip=true" < /dev/null

    ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "cp ~/alluxio-gtcs/conf/alluxio-site.properties.template ~/alluxio-gtcs/conf/alluxio-site.properties" < /dev/null
    
    ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "cp ~/alluxio-gtcs/conf/alluxio-env.sh.template ~/alluxio-gtcs/conf/alluxio-env.sh" < /dev/null

    ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "echo 'alluxio.worker.memory.size=8GB' >> ~/alluxio-gtcs/conf/alluxio-site.properties" < /dev/null

    ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "echo 'alluxio.master.hostname=${k:9}' >> ~/alluxio-gtcs/conf/alluxio-site.properties" < /dev/null

    ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "echo 'alluxio.underfs.address=hdfs://${k:9}:9000'>> ~/alluxio-gtcs/conf/alluxio-site.properties" < /dev/null

    ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "echo ${k:9} >> ~/alluxio-gtcs/conf/masters" < /dev/null

    let "i++"

done < $(cd `dirname $0`; cd ..; pwd)/flintrock.txt