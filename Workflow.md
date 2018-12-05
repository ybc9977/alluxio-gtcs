1. sh  *alluxio-gtcs.dir*/script/gtcs.sh #Workers #Clients

2. sh *alluxio-gtcs.dir*/script/get_log.sh

   resulting in a directory named *gtcs_log* on the Desktop, with master.txt, user_id.txt, OpuS/FairRide logs inside.

3. sh *alluxio-gtcs.dir*/script/destroy.sh




Additional bash script:

stop.sh no parameter: *stop alluxio and clean logs/journal*

start.sh #Worker: *start alluxio with #Worker--number of workers*

add_slave.sh #delta #prev master+worker: *add slaves to the system, delta is the change of user number, the second parameter is the sum of previous master number and worker number*



Configuration on flintrock:

```bash
$ flintrock configure
```



Notice:

It seems that there's some issue on file creating, so we need to maintain the python logs inside the python directory.