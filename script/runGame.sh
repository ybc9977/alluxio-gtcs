
i=1
while test $i -ne 13
do
~/alluxio-gtcs/bin/alluxio runGame 500 300
let "i++"
done