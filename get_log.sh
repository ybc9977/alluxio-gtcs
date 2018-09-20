i=1
while read -r line
do
    ssh -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem ${line} "cd alluxio-gtcs; zip -r $i.zip ./logs" < /dev/null
    scp -o StrictHostKeyChecking=no -i ~/.ssh/gtcs.pem -r ${line}:~/alluxio-gtcs/$i.zip ~/Downloads/
    let i+=1
done < ~/flintrock.txt

