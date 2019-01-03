package alluxio.master.game;

import alluxio.AlluxioURI;
import alluxio.Configuration;
import alluxio.Constants;
import alluxio.client.ReadType;
import alluxio.client.WriteType;
import alluxio.client.file.*;
import alluxio.client.file.options.CreateFileOptions;
import alluxio.client.file.policy.FileWriteLocationPolicy;
import alluxio.collections.Pair;
import alluxio.exception.AlluxioException;
import alluxio.exception.status.AlluxioStatusException;
import alluxio.master.file.FileSystemMaster;
import alluxio.client.file.options.FreeOptions;
import alluxio.PropertyKey;
import alluxio.thrift.ClientNetAddress;
import alluxio.thrift.RegisterUserTOptions;

import alluxio.util.CommonUtils;
import com.google.common.io.Closer;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alluxio.client.file.options.OpenFileOptions;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.DoubleStream;

/**
 *  created by byangak on 28/06/2018
 *
 *  Game System master side processing center
 */

public final class GameSystemMaster {

    private static final Logger LOG = LoggerFactory.getLogger(GameSystemMaster.class);

    ///** timer to start the thread */
    //public timer t = new timer();



    /** use integers to present file ids: faster and smaller space
     *  The actual file path will simply be <AlluxioFolder>/<file_id>.
     * **/

    private static String AlluxioFolder = "/test";
    private static List<Double> cachedRatio = new ArrayList<>(); // cache ratio of files

    /**
     * Access factors of users.
     * In game, they are all ones for all users;
     * under OpuS, a user has the same factor for all files;
     * under FairRide, a user has file-specific factors.
     *
     * The order is the same as the userList.
     */
    private static List<List<Double>> accessFactors = new ArrayList<>();

    /** Each userId is String */
    private static List<String> userList = new ArrayList<>();

    /** Map from user ID to gameSystemClient */
    private static Map<String, GameSystemClient> clientMap = new LinkedHashMap<>();

    /** User and their corresponding file preference map*/
    private static Map<String, List<Double>> userPref = new LinkedHashMap<>();

    private static String currentDirectory = System.getProperty("user.dir");

    private static File log = new File (currentDirectory + "/alluxio-gtcs/master.txt");

    private static FileSystem fileSystem; // to do load and free


    private static int Total_QUOTA;// = Configuration.getInt(PropertyKey.CACHE_QUOTA);
    private static int File_Number;//= Configuration.getInt(PropertyKey.FILE_NUMBER);
    private static int File_Size= Configuration.getInt(PropertyKey.FILE_SIZE); // file size in MB

    private enum MODE {OpuS,FairRide,Game}

    public GameSystemMaster(){
        fileSystem = FileSystem.Factory.get();
        try{
            if (!log.exists())
                log.createNewFile();
        }catch(IOException e){
            e.printStackTrace();
        }

    }



//    /** add file into fileList & cacheList */
//    public synchronized static void addfile(Integer id){
//        fileList.put(id,false);
//        cacheList.put(id,false);
//        LOG.info("File" + id + "has been added successfully: ");
//    }

    /** add user through user side server register request
     * @param userId user app_ID sent from
     */
    private synchronized static void adduser(String userId) {
        if(!userList.contains(userId)){
            userList.add(userId);
            //cacheMap.put(userId,null);
            LOG.info("user "+ userId +" is added successfully into userList");
            System.out.println("After add: " + userList.size() + " users.");

        }else{
            LOG.info("user "+ userId +" is already in the userList");
        }


    }

    /** user registration, called the moment when the client side server start
     * @param userId USER_APP_ID
     * @param hostname client side server hostname
     * @param address client side server address, contains RPC_host & RPC_port */
    public synchronized static void register(String userId, String hostname, ClientNetAddress address, RegisterUserTOptions options){
        adduser(userId);
        InetSocketAddress mAddress = new InetSocketAddress(hostname, address.getRpcPort());
        LOG.info("register a client with : " + mAddress.getAddress().toString());
        GameSystemClient client = new GameSystemClient(null,mAddress,userId);
        clientMap.put(userId,client);
    }

    /** Run OpuS and launch access **/
    private static long runOpuS() throws IOException, InterruptedException {

        // run OpuS
        ArrayList<String> cmd = new ArrayList<>();
        cmd.add("python");
        cmd.add(currentDirectory + "/alluxio-gtcs/python/OpuS.py");

        Object[] objectList = cmd.toArray();
        String[] cmdArray = Arrays.copyOf(objectList, objectList.length, String[].class);

        Long start_time = System.currentTimeMillis();

        Process p= Runtime.getRuntime().exec(cmdArray); // exec is non-blocking, add wait-for
        p.waitFor();

        Long time = System.currentTimeMillis()-start_time;

        // read cache ratios
        File file = new File(currentDirectory+"/alluxio-gtcs/python/ratio_opus.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        for(String ratioStr: br.readLine().split(",")){
            Double ratio = Double.parseDouble(ratioStr);
            cachedRatio.add(ratio);
        }
        br.close();

        cacheOrFree();

        // read access factors
        file = new File(currentDirectory+"/alluxio-gtcs/python/factor_opus.txt");
        br = new BufferedReader(new FileReader(file));

        // each user has a single factor for all files
        String[] factorStrs= br.readLine().split(",");
        for(int i=0; i<userList.size();i++){
            Double factor = Double.parseDouble(factorStrs[i]);
            Double[] factors = new Double[File_Number];
            Arrays.fill(factors, factor);
            accessFactors.add(Arrays.asList(factors));
        }

        Double hitRatio = calculateHitRatio();

        // launch clients to access
        Pair<Double,Double> pair = access(MODE.OpuS);

        FileOutputStream fop = new FileOutputStream(log,true);
        OutputStreamWriter writer = new OutputStreamWriter(fop);
        writer.write("OpuS\n Runtime\t" + time + "\t Expect HR\t" + hitRatio + "\t Experiment HR\t" + pair.getFirst() + "\t Latency\t" + pair.getSecond() + "\n");
        writer.close();
        fop.close();

        return time;
    }

    /** Run FairRide and launch access**/
    private static long runFairRide() throws IOException, InterruptedException {

        // Run fairride
        ArrayList<String> cmd = new ArrayList<>();
        cmd.add("python");
        cmd.add(currentDirectory + "/alluxio-gtcs/python/FairRide.py");

        Object[] objectList = cmd.toArray();
        String[] cmdArray = Arrays.copyOf(objectList, objectList.length, String[].class);

        Long start_time = System.currentTimeMillis();

        Process p=Runtime.getRuntime().exec(cmdArray);
        p.waitFor();

        Long time = System.currentTimeMillis()-start_time;

        // read cache ratios
        cachedRatio.clear();
        File file = new File(currentDirectory+"/alluxio-gtcs/python/ratio_fairride.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        for(String ratioStr: br.readLine().split(",")){
            Double ratio = Double.parseDouble(ratioStr);
            cachedRatio.add(ratio);
        }
        br.close();

        cacheOrFree();

        // read access factors
        accessFactors.clear();
        file = new File(currentDirectory+"/alluxio-gtcs/python/factor_fairride.txt");
        br = new BufferedReader(new FileReader(file));
        String str;
        while((str = br.readLine()) != null){
            List<Double> factors = new ArrayList<>();
            for(String factorStr: br.readLine().split(","))
                factors.add(Double.parseDouble(factorStr));
            accessFactors.add(factors);
        }

        Double hitRatio = calculateHitRatio();

        Pair<Double,Double> pair = access(MODE.FairRide);

        FileOutputStream fop = new FileOutputStream(log,true);
        OutputStreamWriter writer = new OutputStreamWriter(fop);
        writer.write("FairRide\n Runtime\t" + time + "\t Expect HR\t" + hitRatio + "\t Experiment HR\t" + pair.getFirst() + "\t Latency\t" + pair.getSecond() + "\n");
        writer.close();
        fop.close();
        //return new Pair<>(time, hitRatio); // runtime and expected hit ratio
        return time;
    }


    /**
     *  Cache or free files based on the cachedRatio
     *
     */
    private static void cacheOrFree(){
        for(int i=0; i<File_Number;i++){
            AlluxioURI alluxioURI = new AlluxioURI(AlluxioFolder + "/" + i);
            try {
                int inMemoryPercentage = fileSystem.getStatus(alluxioURI).getInMemoryPercentage();

                if(cachedRatio.get(i)>0 && inMemoryPercentage<100){ // do full load
                    OpenFileOptions options = OpenFileOptions.defaults().setReadType(ReadType.CACHE_PROMOTE);
                    Closer closer = Closer.create();
                    try {
                        FileInStream in = closer.register(fileSystem.openFile(alluxioURI, options));
                        byte[] buf = new byte[8 * Constants.MB];
                        while (in.read(buf) != -1) {} // read the file with "Cache_promote" option
                    } catch (Exception e) {
                        throw closer.rethrow(e);
                    } finally {
                        closer.close();
                    }
                    System.out.println("File " + i + " has been loaded.");
                }


                if(cachedRatio.get(i)==0 && inMemoryPercentage>0){ // do full free
                    fileSystem.free(alluxioURI,FreeOptions.defaults());
                    System.out.println("File " + i + " has been freed.");
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * Get user's preference from game clients
     */
    private static void getPref() throws AlluxioStatusException{
        for(String userID: userList){
            GameSystemClient client = clientMap.get(userID);
            userPref.put(userID, client.updatePref(File_Number));
        }

        // write preferences into prefs.txt
        System.out.println("Current dir:" + currentDirectory);
        File prefLog = new File(currentDirectory+"/alluxio-gtcs/python/prefs.txt");


        try {
            if (!prefLog.exists())
                prefLog.createNewFile();
            FileOutputStream fop = new FileOutputStream(prefLog,false);
            OutputStreamWriter writer = new OutputStreamWriter(fop);
            writer.write(String.valueOf(Total_QUOTA)+'\n');
            for(String userId: userList){
                List<Double> prefs = userPref.get(userId);
                writer.write(Arrays.toString(prefs.toArray()) // [1.0, 2.0, ..., ]
                        .replace("[", "")  //remove the right bracket
                        .replace("]", "")  //remove the left bracket
                        //.replace(",", "\t")
                        .trim()); // remove trailing spaces from partially initialized arrays
                writer.write("\n");
            }
            writer.flush();
            writer.close();
            fop.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Create files if not exist. From file 0 to file (File_Number-1).
     */
    private static void initWrite() throws AlluxioException, IOException{
        for(int fileId=0;fileId < File_Number;fileId++) {
            AlluxioURI alluxioURI = new AlluxioURI(AlluxioFolder + "/" + fileId);
            if (!fileSystem.exists(alluxioURI)) {
                System.out.println("Start to write file "+ fileId + " of size " + File_Size + " MB.");
                FileOutStream os = fileSystem.createFile(alluxioURI,
                        CreateFileOptions.defaults().setWriteType(WriteType.CACHE_THROUGH));
                byte[] buf = new byte[8 * 1024*1024];
                long bytes = File_Size * 1024 * 1024;
                int writeBytes;
                while (bytes > 0) {
                    writeBytes = (int) Math.min(bytes, buf.length);
                    os.write(buf, 0, writeBytes);
                    bytes -= writeBytes;
                }
                os.close();
            }
            else{
                System.out.println("File "+ fileId + " already exists.");
            }
        }
    }

    /** the main logic of cache sharing game */
    private static Pair<Integer, Long> game() throws AlluxioStatusException, IOException {

        boolean[] cacheFlag = new boolean[File_Number];
        int quota = Total_QUOTA / clientMap.size();
        Long start_time =System.currentTimeMillis();

        int pollIter = 0;
        int unchange = 0; // the number of users whose decisions do not change. Reset to zero once any user changes its decisions

        while(unchange < userList.size()){
            String userId = userList.get(pollIter % userList.size());
            GameSystemClient client = clientMap.get(userId);
            if(client.poll(cacheFlag, quota))
                unchange ++;
            else
                unchange =0;
            pollIter ++;
        }
        Long time = System.currentTimeMillis()-start_time;

        // set cache ratios
        cachedRatio.clear();
        for(boolean flag: cacheFlag){
            if(flag)
                cachedRatio.add(1.0);
            else
                cachedRatio.add(0.0);
        }


        // set access factors: all ones!
        accessFactors.clear();
        Double[] ones = new Double[File_Number];
        Arrays.fill(ones,1.0);
        for(int i=0;i<userList.size();i++)
            accessFactors.add(Arrays.asList(ones));

        cacheOrFree();
        Double hitRatio = calculateHitRatio();

        Pair<Double,Double> pair = access(MODE.Game); // new Pair<>(-1.0,-1.0);

        FileOutputStream fop = new FileOutputStream(log,true);
        OutputStreamWriter writer = new OutputStreamWriter(fop);
        writer.write("Game\n Runtime\t" + time + "\t Iteration number\t" + pollIter+ "\t Expect HR\t" + hitRatio + "\t Experiment HR\t" + pair.getFirst() + "\t Latency\t" + pair.getSecond() + "\n");
        writer.close();
        fop.close();

        return new Pair<>(pollIter, time);

//        while(userList.size()!=0 && !fileList.isEmpty()){
//            int userPos = poll_iter % userList.size();
//            Pair<String, Boolean> user = userList.get(userPos);
//            String userID = user.getFirst();
//
//            // file list pre-treatment
//            user.setSecond(false);
//            if(cacheMap.get(userID)!=null){
//                for (String file: cacheMap.get(userID)){
//                    fileList.put(file,false);
//                }
//            }
//            GameSystemClient client = clientList.get(userID);
//            List<String> caching_list = client.checkCacheChange(fileList,quota);
//
//
//
//            // Check if caching_list stay the same with cacheMap, if so, do "else"
//
//            if(cacheMap.get(userID)!=null){
//                for (String key:caching_list){
//                    if(!cacheMap.get(userID).contains(key)){
//                        cacheMap.put(userID,caching_list);
//                        user.setSecond(true);
//                        break;
//                    }
//                }
//            }else{
//                cacheMap.put(userID,caching_list);
//                user.setSecond(true);
//            }
//            for (String file_path : caching_list) {
//                fileList.put(file_path,true);
//            }
//            userList.set(userPos,user);
//
//            int count = 0;
//            poll_iter++;
//            for (Pair<String,Boolean> p:userList) {
//                // check whether the user changed his decision or not
//                if (p.getSecond()) {
//                    break;
//                }else{
//                    count++;
//                }
//                if (count == userList.size()){
//                    LOG.info("Equilibrium established");
//                    LOG.info("Total iteration: "+poll_iter);
//                    long t = System.currentTimeMillis()-start_time;
//                    LOG.info("Total time cost(ms): "+t+" ms");
//                    try {
//                        FileOutputStream fop = new FileOutputStream(log,true);
//                        OutputStreamWriter writer = new OutputStreamWriter(fop);
//                        writer.write("Polling Time: "+t+" ms\n");
//                        writer.write("Iteration: "+poll_iter+"\n");
//                        writer.flush();
//                        writer.close();
//                        fop.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    // file caching process
//                    for (String U: clientList.keySet()){
//                        clientList.get(U).cacheIt(cacheMap.get(U),fileList,cacheList,fileSystemMaster);
//                        clientList.get(U).reset();
//                    }
//                    // calculate theoretical efficiency
//                    Efficiency(QUOTA);
//                    // simulate the access process and get hit ratio
//                    HitRatio();
//                    // file read and calculate hit ratio
//                    Access();
//                    completion = true;
//                    break;
//                }
//            }
//            if(completion)
//                break;
////            LOG.info("Iter num: " + poll_iter + " Time cost : " + (System.currentTimeMillis()-iter_start));
//            if (poll_iter%userList.size()==0){
//                Collections.shuffle(userList);
//            }
//        }

        // prepare the preference file list for the next OpuS or FairRide run
//
//        for (int i=0;i<userList.size();i++){
//            prefFileList.add(i,new ArrayList<>());
//        }
//
//        int i = 0;
//        for (String user : userPref.keySet()){
//            prefFileList.get(i).addAll(userPref.get(user).keySet());
//            i++;
//        }

        // write preferences into prefs.txt
//        File prefLog = new File(currentDirectory+"/alluxio-gtcs/python/prefs.txt");
//        try {
//            if (!prefLog.exists())
//                prefLog.createNewFile();
//            FileOutputStream fop = new FileOutputStream(prefLog,false);
//            OutputStreamWriter writer = new OutputStreamWriter(fop);
//            writer.write(String.valueOf(Total_QUOTA)+'\n');
//            for(Map.Entry<String,List<Double>> entry : userPref.entrySet()){
//                List<Double> prefs = entry.getValue();
//                writer.write(Arrays.toString(prefs.toArray()) // [1.0, 2.0, ..., ]
//                        .replace("[", "")  //remove the right bracket
//                        .replace("]", "")  //remove the left bracket
//                        .trim()); // remove trailing spaces from partially initialized arrays
//            }
//            writer.flush();
//            writer.close();
//            fop.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        // Comparison with OpuS and FairRide
//        try {
//            freeAll();
//            FileOutputStream f = new FileOutputStream(log,true);
//            OutputStreamWriter w = new OutputStreamWriter(f);
//            w.write("OpuS: \n");
//            w.flush();
//            LOG.info("Start OpuS");
//            OpuSComparasion();
//
//            freeAll();
//            w.write("FairRide: \n");
//            w.flush();
//            LOG.info("Start FairRide");
//            FairRideComparison();
//
//            w.write("\n");
//            w.flush();
//            w.close();
//            freeAll();
//
//            // reset two lists
//            for (String file:fileList.keySet()){
//                fileList.replace(file,false);
//            }
//            for (String file:cacheList.keySet()){
//                cacheList.replace(file,false);
//            }
//        } catch (FileDoesNotExistException | InvalidPathException | UnexpectedAlluxioException | AccessControlException | IOException | InterruptedException e) {
//            e.printStackTrace();
//        }

    }

    /**
     * Calculate the optimal cache hit ratio (LFU)
     */
    private static void optimalHitRatio() throws IOException{
        if(Total_QUOTA >= File_Number)
            return; // 100% hr

        Double[] totalPref = new Double[File_Number];
        Arrays.fill(totalPref, 0.0);
        //System.out.println("File_Number:" + File_Number + "Cache quota:" + Total_QUOTA);
        for(List<Double> prefs: userPref.values()){
            for(int i = 0; i< totalPref.length;i++)
                totalPref[i] += prefs.get(i);
        }
        //sort it
        Arrays.sort(totalPref,Collections.reverseOrder());

        Double[] cachedPref = Arrays.copyOfRange(totalPref,0,Total_QUOTA);

        Double hitRatio = DoubleStream.of(ArrayUtils.toPrimitive(cachedPref)).sum() / DoubleStream.of(ArrayUtils.toPrimitive(totalPref)).sum();
        FileOutputStream fop = new FileOutputStream(log,true);
        OutputStreamWriter writer = new OutputStreamWriter(fop);
        writer.write("Optimal HR\t" + hitRatio + "\n");
        writer.close();
        fop.close();
    }


    /**
     * Calculate the expected cache hit ratio based on the current #cachedRatio#
     */
    private static double calculateHitRatio() {
        Double cachedSum =0.0;
        Double sum = 0.0;
        for(int i = 0; i< File_Number;i++){
            Double thisSum = 0.0;
            for(List<Double> prefs: userPref.values())
                thisSum += prefs.get(i);
            sum += thisSum;
            cachedSum += thisSum * cachedRatio.get(i);
        }

        return cachedSum / sum;
    }


    /**
     * Launch client to access, in parallel.
     * Collect the average cache hit ratio and read latency from each client with future variables
     * @throws AlluxioStatusException
     */
    private static Pair<Double, Double> access(MODE mode) throws AlluxioStatusException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Future<Pair<Double, Long>>> futureResults = new ArrayList<>();
        String m="";
        if(mode ==MODE.FairRide)
            m="FairRide";
        else if(mode == MODE.OpuS)
            m = "OpuS";
        else if(mode == MODE.Game)
            m = "Game";

        for (int i=0; i<userList.size();i++) {
            GameSystemClient client = clientMap.get(userList.get(i)); // the i(th) client in the userList
            Future<Pair<Double, Long>> future = executorService.submit(new LaunchAccessThread(client, m, cachedRatio, accessFactors.get(i)));
            futureResults.add(future); // client.access() returns average hit ratio and latency.
        }

        double totalHit=0;
        double totalLatency=0; // use a double instead to allow potential fractions.

        // now get the results: block
        try{
            for (Future<Pair<Double, Long>> futureResult:futureResults){
                Pair<Double, Long> pair =futureResult.get();
                totalHit += pair.getFirst();
                totalLatency += pair.getSecond();
            }
        }catch(InterruptedException | ExecutionException e){
            e.printStackTrace();
        }
        return new Pair<>(totalHit/userList.size(), totalLatency/userList.size());

//        try {
//            FileOutputStream fop = new FileOutputStream(log,true);
//            OutputStreamWriter writer = new OutputStreamWriter(fop);
//            writer.write("Actual Hit Ratio: " + ratio + "\n");
//            writer.write("Average Access Latency: " + (double) time/accessNum + " ms\n");
//            writer.flush();
//            writer.close();
//            fop.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        LOG.info("the actual hit ratio for game is " + ratio);
//        LOG.info("the average overall access time for game is " + time + " ms");
    }

//    private void Access_OpuS() throws IOException {
//        ArrayList<Pair<Double,Long>> accessList = new ArrayList<>();
//        for (GameSystemClient client:clientList.values()){
//            accessList.add(client.access("OpuS"));
//        }
//        double sum1 = 0;
//        double sum2 = 0;
//        DataInputStream f = new DataInputStream(new FileInputStream(currentDirectory + "/alluxio-gtcs/python/factor_opus.txt"));
//        Scanner sc = new Scanner(f);
//        String str = sc.nextLine().replaceAll("\\s","");
//        String[] values = new String[userList.size()];
//        int i = 0;
//        for (int j=0;j<userList.size();j++){
//            values[j]="";
//        }
//        for (char c: str.toCharArray()){
//            if(c==',') {
//                i++;
//            }else{
//                values[i]+=c;
//            }
//        }
//        long sum3=0L, time=0L;
//        for (Pair<Double, Long> anAccessList : accessList) {
//            Double factor = Double.parseDouble(values[accessList.indexOf(anAccessList)]);
//            sum1 += anAccessList.getFirst() * factor;
//            sum2 += anAccessList.getFirst();
//            sum3 += anAccessList.getSecond();
//        }
//        double ratio1 = sum1/accessList.size();
//        double ratio2 = sum2/accessList.size();
//        time = sum3/accessList.size();
//        try {
//            FileOutputStream fop = new FileOutputStream(log,true);
//            OutputStreamWriter writer = new OutputStreamWriter(fop);
//            writer.write("Factorized Hit Ratio: " + ratio1 + "\n");
//            writer.write("Normal Hit Ratio: " + ratio2 + "\n");
//            writer.write("Average Access Latency: " + (double) time/accessNum + " ms\n");
//            writer.flush();
//            writer.close();
//            fop.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        LOG.info("the factorized hit ratio for OpuS is "+ ratio1);
//        LOG.info("the normal hit ratio for OpuS is "+ ratio2);
//        LOG.info("the average overall access time for OpuS is " + time + " ms");
//        f.close();
//    }
//
//    private void Access_FairRide() throws IOException {
//        ArrayList<Pair<Double, Long>> accessList = new ArrayList<>();
//        DataInputStream f = new DataInputStream(new FileInputStream(currentDirectory + "/alluxio-gtcs/python/factor_fairride.txt"));
//        Scanner sc = new Scanner(f);
//        for (GameSystemClient client:clientList.values()){
//            ArrayList<Double> factorList = new ArrayList<>();
//            String str = sc.nextLine().replaceAll("\\s","");
//            String[] values = new String[fileList.size()];
//            int i = 0;
//            for (int j=0;j<fileList.size();j++){
//                values[j]="";
//            }
//            for (char c: str.toCharArray()){
//                if(c==',') {
//                    i++;
//                }else{
//                    values[i]+=c;
//                }
//            }
//            LOG.info(Arrays.toString(values));
//            for (String s:values){
//                if(!s.isEmpty()){
//                    Double d = Double.parseDouble(s);
//                    factorList.add(d);
//                }
//            }
//            accessList.add(client.accessFairRide(factorList));
//        }
//        double sum1 = 0;
//        long sum2 = 0;
//        for (Pair<Double,Long> access : accessList) {
//            sum1 += access.getFirst();
//            sum2 += access.getSecond();
//        }
//        double ratio = sum1/accessList.size();
//        long time = sum2/accessList.size();
//        try {
//            FileOutputStream fop = new FileOutputStream(log,true);
//            OutputStreamWriter writer = new OutputStreamWriter(fop);
//            writer.write("Factorized Hit Ratio: " + ratio + "\n");
//            writer.write("Average Access Latency: " + (double)time/accessNum + " ms\n");
//            writer.flush();
//            writer.close();
//            fop.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        LOG.info("the factorized hit ratio for FairRide is "+ ratio);
//        LOG.info("the average overall access time for FairRide is " + time + " ms");
//        f.close();
//    }


//    /**
//     * Sort the Map by descending order according to the values
//     */
//    private <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
//
//        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
//        list.sort(Map.Entry.comparingByValue());
//        for (int i = 0; i < Math.floor(list.size() / 2) ; i++){
//            Map.Entry<K, V> temp;
//            temp = list.get(i);
//            list.set(i,list.get(list.size()-i-1));
//            list.set(list.size()-i-1,temp);
//        }
//        Map<K, V> result = new LinkedHashMap<>();
//        for (Map.Entry<K, V> entry : list) {
//            result.put(entry.getKey(), entry.getValue());
//        }
//        return result;
//    }

    /**
     * Run game, opus and fairride in sequence and log the results.
     *
     * Only run it after all users have registered and updated their prefs!
     */
    public static void runAll(int fileNumber, int quota){
        File_Number = fileNumber;
        Total_QUOTA = quota;

        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();
        System.out.println("Current absolute path is: " + s);
        currentDirectory = s;

        fileSystem = FileSystem.Factory.get();
        try{
            if (!log.exists())
                log.createNewFile();
        }catch(IOException e){
            e.printStackTrace();
        }

        try {
            getPref(); //get preferences from clients and also log to pref.txt

            initWrite();

            optimalHitRatio();

            Pair<Integer, Long> result = game();
            LOG.info("Game runtime (ms):" + result.getSecond());
            LOG.info("Game iteration #:" + result.getFirst());

            Long runtime = runOpuS();
            LOG.info("OpuS runtime (ms):" + runtime);


            runtime = runFairRide();
            LOG.info("FairRide runtime (ms):" + runtime);

        } catch(Exception e){
            e.printStackTrace();
        }


    }



//    /** timer task to start GTC*/
//    public class timer extends TimerTask {
//        @Override
//        public void run() {
//                try {
//                    LOG.info("Start Game");
//                    game();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//            }
//        }

//    private void freeAll() throws FileDoesNotExistException, InvalidPathException,
//            AccessControlException, UnexpectedAlluxioException, IOException {
//        for (String file : cacheList.keySet()){
//            if (cacheList.get(file)){
//                fileSystemMaster.free(new AlluxioURI(file), FreeOptions.defaults());
//                LOG.info("Free Process Complete, uri: " + file);
//            }
//        }
//        LOG.info("FreeAll Process Complete" );
//    }

}
