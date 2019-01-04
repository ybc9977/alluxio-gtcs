1. launch the cluster and install required packages 
  sh  *alluxio-gtcs.dir*/script/launch.sh #Workers #Clients

2. set up the alluxio configurations (master &  workers) and launch Alluxio
  sh script/start.sh #Workers

3. start clients (will kill the existing ones first)

   sh script/runClients.sh #Workers

4. check whether the clients have all registered
  python3 flintrock/standalone.py login gtcs
  [On Master] vim ~/alluxio-gtcs/logs/master.out

5. start the game (and opus and fairride)
  [On Master] ~/alluxio-gtcs/bin/alluxio runGame #Files #TotalQuota

6. update the preferences of some clients

  [On Master] ~/alluxio-gtcs/bin/alluxio updatePrefComp #Files #TotalQuota #Updates #Loops

7. sh *alluxio-gtcs.dir*/script/get_log.sh

   resulting in a directory named *gtcs_log* on the Desktop, with master.txt, user_id.txt, OpuS/FairRide logs inside.

8. sh *alluxio-gtcs.dir*/script/destroy.sh




Additional bash script:

stop.sh no parameter: *stop alluxio and clean logs/journal*

start.sh #Worker: *start alluxio with #Worker--number of workers*

add_slave.sh #delta #prev master+worker: *add slaves to the system, delta is the change of user number, the second parameter is the sum of previous master number and worker number*



Configuration on flintrock:

```bash
$ flintrock configure
```



