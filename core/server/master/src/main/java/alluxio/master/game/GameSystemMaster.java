package alluxio.master.game;

import alluxio.AlluxioURI;
import alluxio.PropertyKey;
import alluxio.collections.Pair;
import alluxio.exception.AccessControlException;
import alluxio.exception.FileDoesNotExistException;
import alluxio.exception.InvalidPathException;
import alluxio.exception.UnexpectedAlluxioException;
import alluxio.exception.status.AlluxioStatusException;
import alluxio.master.file.FileSystemMaster;
import alluxio.master.file.options.FreeOptions;
import alluxio.thrift.ClientNetAddress;
import alluxio.thrift.RegisterUserTOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  created by byangak on 28/06/2018
 *
 *  Game System master side processing center
 */

public final class GameSystemMaster {

    private static final Logger LOG = LoggerFactory.getLogger(GameSystemMaster.class);

    /** timer to start the thread */
    public timer t = new timer();

    /** ClientList to store master side clients, to communicate with certain client side server */
    private static Map<String, GameSystemClient> clientList = new HashMap<>();

    /** Mapping userId to the files it cached */
    private static Map<String,List<String>> cacheMap = new HashMap<>();

    /** FileList which is unsettled during the querying process */
    private static Map<String,Boolean> fileList = new ConcurrentHashMap<>();

    /** FileList which is settled and always representing the realistic circumstance */
    private static Map<String,Boolean> cacheList = new HashMap<>();

    /** Each user is a Pair contains userId(Long) and isCachingOptionChanged(Boolean) */
    private static ArrayList<Pair<String,Boolean>> userList = new ArrayList<>();

    private static Map<String,Double> utilList = new HashMap<>();

    private static Map<String, Map<String, Double>> userPref = new HashMap<>();

    private static ArrayList<ArrayList<String>> prefFileList = new ArrayList<>();

    private static String currentDirectory = System.getProperty("user.dir");

    private File log = new File (currentDirectory + "/alluxio-gtcs/master.txt");

    private FileSystemMaster fileSystemMaster;

    private static long start_time;

    private static int QUOTA;

    public GameSystemMaster(FileSystemMaster defaultFileSystemMaster) throws IOException {
        fileSystemMaster = defaultFileSystemMaster;
        if (!log.exists())
            log.createNewFile();
    }


    /** add file into fileList & cacheList */
    public synchronized static void addfile(String path){
        fileList.put(path,false);
        cacheList.put(path,false);
        LOG.info("a file has been added successfully: " + path );
    }

    /** add user through user side server register request
     * @param userId user app_ID sent from
     */
    private static void adduser(String userId) {
        Pair<String,Boolean> p1 = new Pair<>(userId, true);
        Pair<String,Boolean> p2 = new Pair<>(userId, false);
        if(!userList.contains(p1)&&!userList.contains(p2)){

            Pair<String, Boolean> pair = new Pair<>(userId, false);
            userList.add(pair);
            cacheMap.put(userId,null);
            LOG.info("user "+ userId +" is added successfully into userList");

        }else{
            LOG.info("user "+ userId +" is already in the userList");
        }
    }

    /** when external command is sent, change corresponding file status in the fileList & cacheList
     * currently has just implemented free file circumstance
     * @param path file path
     * @param isCached whether the file is cached or not */
    public static void changeFileMode(final String path, final boolean isCached){
        if(cacheList.get(path)!=isCached){
            cacheList.replace(path,isCached);
            fileList.replace(path,isCached);
        }
    }

    /** user registration, called the moment when the client side server start
     * @param userId USER_APP_ID
     * @param hostname client side server hostname
     * @param address client side server address, contains RPC_host & RPC_port */
    public static void register(String userId, String hostname, ClientNetAddress address, RegisterUserTOptions options){
        adduser(userId);
        InetSocketAddress mAddress = new InetSocketAddress(hostname, address.getRpcPort());
        LOG.info("register a client with : " + mAddress.getAddress().toString());
        GameSystemClient client = new GameSystemClient(null,mAddress,userId);
        clientList.put(userId,client);
    }

    /** the comparison thread for running OpuS plugged into this System **/
    private synchronized void OpuSComparasion() throws IOException {


        ArrayList<String> cmd = new ArrayList<>();
        cmd.add("python");
        cmd.add(currentDirectory + "/alluxio-gtcs/python/OpuS.py");

        Object[] objectList = cmd.toArray();
        String[] cmdArray = Arrays.copyOf(objectList, objectList.length, String[].class);

        Long start_time = System.currentTimeMillis();

        Runtime.getRuntime().exec(cmdArray);

        Long time = System.currentTimeMillis()-start_time;
        LOG.info("OpuS Execution Time(ms): " + time );
        FileOutputStream fop = new FileOutputStream(log,true);
        OutputStreamWriter writer = new OutputStreamWriter(fop);
        writer.write("Execution Time:"+time+"\n");
        writer.flush();
        writer.close();
        fop.close();

        DataInputStream f = null;
        boolean b = true;
        int count = 0;
        while (b || count == 100){
            try {
                f = new DataInputStream(new FileInputStream(currentDirectory+"/alluxio-gtcs/python/ratio_opus.txt"));
                readAndCache(f, fileSystemMaster);
                f.close();
                b = false;
            } catch (IOException e) {
                e.printStackTrace();
                count++;
            }
        }

        LOG.info("Comparative OpuS result: ");
        Efficiency(QUOTA);
        HitRatio();
        Access_OpuS();
    }

    /** the comparison thread for running OpuS plugged into this System **/
    private synchronized void FairRideComparison() throws IOException {


        ArrayList<String> cmd = new ArrayList<>();
        cmd.add("python");
        cmd.add(currentDirectory + "/alluxio-gtcs/python/FairRide.py");

        Object[] objectList = cmd.toArray();
        String[] cmdArray = Arrays.copyOf(objectList, objectList.length, String[].class);

        Long start_time = System.currentTimeMillis();

        Runtime.getRuntime().exec(cmdArray);

        Long time = System.currentTimeMillis()-start_time;
        LOG.info("FairRide Execution Time(ms): " + time );
        FileOutputStream fop = new FileOutputStream(log,true);
        OutputStreamWriter writer = new OutputStreamWriter(fop);
        writer.write("Execution Time:"+time+"\n");
        writer.flush();
        writer.close();
        fop.close();

        DataInputStream f = null;
        boolean b = true;
        int count = 0;
        while (b || count == 100){
            try {
                f = new DataInputStream(new FileInputStream(currentDirectory+"/alluxio-gtcs/python/ratio_fairride.txt"));
                readAndCache(f, fileSystemMaster);
                f.close();
                b = false;
            } catch (IOException e) {
                e.printStackTrace();
                count++;
            }
        }

        LOG.info("Comparative FairRide result: ");
        Efficiency(QUOTA);
        HitRatio();
        Access_FairRide();

    }

    private static void readAndCache(DataInputStream f, FileSystemMaster fileSystemMaster) throws IOException {
        Map<String, Double> cacheList = new HashMap<>();
        ArrayList<Double> ratioList = new ArrayList<>();
        Scanner sc = new Scanner(f);
        String str = sc.nextLine().replaceAll("\\s","");
        String[] values = new String[fileList.size()];
        int i = 0;
        for (int j=0;j<fileList.size();j++){
            values[j]="";
        }
        for (char c: str.toCharArray()){
            if(c==',') {
                i++;
            }else{
                values[i]+=c;
            }
        }
        LOG.info(Arrays.toString(values));
        for (String s:values){
            if(!s.isEmpty()){
                Double d = Double.parseDouble(s);
                ratioList.add(d);
            }
        }
        ArrayList<String> cList = new ArrayList();
        for (int j = 0; j < clientList.size(); j++) {
            for (String file : prefFileList.get(j)) {
                cacheList.put(file, ratioList.get(prefFileList.get(j).indexOf(file)));
                if(ratioList.get(prefFileList.get(j).indexOf(file))!=0){
                    cList.add(file);
                }
            }
        }
        //Logic 1
        for (String file:cacheList.keySet()){
            String[] keys = clientList.keySet().toArray(new String[0]);
            Random random = new Random();
            String randomKey = keys[random.nextInt(keys.length)];
            clientList.get(randomKey).cacheIt(file,cacheList,fileSystemMaster);
        }
        //Logic 2
//        i = 0;
//        int j = 0;
//        String[] keys = clientList.keySet().toArray(new String[0]);
//        for (String file : cList){
//            clientList.get(keys[i]).cacheIt(file,cacheList,fileSystemMaster);
//            if (j%(Math.ceil((double)cList.size()/(double)clientList.size()))!=0 && j!=0){
//                i++;
//            }
//            j++;
//        }

    }

    /** the main thread of game theoretical communication, run every 20 sec */
    private synchronized void gameTheoreticalCommunication() throws AlluxioStatusException {
        try {
            FileOutputStream fop = new FileOutputStream(log,true);
            OutputStreamWriter writer = new OutputStreamWriter(fop);
            writer.write("Game: \n");
            writer.flush();
            writer.close();
            fop.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        QUOTA = 300 / clientList.size();

        start_time =System.currentTimeMillis();
        int poll_iter = 0;
        Collections.shuffle(userList);
        boolean completion = false;

        while(userList.size()!=0 && !fileList.isEmpty()){
            Long iter_start = System.currentTimeMillis();
            int userPos = poll_iter % userList.size();
            Pair<String, Boolean> user = userList.get(userPos);
            String userID = user.getFirst();

            // file list pre-treatment
            user.setSecond(false);
            if(cacheMap.get(userID)!=null){
                for (String file: cacheMap.get(userID)){
                    fileList.put(file,false);
                }
            }
            GameSystemClient client = clientList.get(userID);
            List<String> caching_list = client.checkCacheChange(fileList,QUOTA);

            userPref.put(userID,client.getPref());

            // Check if caching_list stay the same with cacheMap, if so, do "else"

            if(cacheMap.get(userID)!=null){
                for (String key:caching_list){
                    if(!cacheMap.get(userID).contains(key)){
                        cacheMap.put(userID,caching_list);
                        user.setSecond(true);
                        break;
                    }
                }
            }else{
                cacheMap.put(userID,caching_list);
                user.setSecond(true);
            }
            for (String file_path : caching_list) {
                fileList.put(file_path,true);
            }
            userList.set(userPos,user);

            int count = 0;
            poll_iter++;
            for (Pair<String,Boolean> p:userList) {
                if (p.getSecond()) {
                    break;
                }else{
                    count++;
                }
                if (count == userList.size()){
                    LOG.info("Equilibrium established");
                    LOG.info("Total iteration: "+poll_iter);
                    long t = System.currentTimeMillis()-start_time;
                    LOG.info("Total time cost(ms): "+t);
                    try {
                        FileOutputStream fop = new FileOutputStream(log,true);
                        OutputStreamWriter writer = new OutputStreamWriter(fop);
                        writer.write("Time: "+t+"\n");
                        writer.write("Iteration: "+poll_iter+"\n");
                        writer.flush();
                        writer.close();
                        fop.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    for (String U: clientList.keySet()){
                        clientList.get(U).cacheIt(cacheMap.get(U),fileList,cacheList,fileSystemMaster);
                        clientList.get(U).reset();
                    }
                    Efficiency(QUOTA);
                    HitRatio();
                    Access();
                    completion = true;
                    break;
                }
            }
            if(completion)
                break;
//            LOG.info("Iter num: " + poll_iter + " Time cost : " + (System.currentTimeMillis()-iter_start));
            if (poll_iter%userList.size()==0){
                Collections.shuffle(userList);
            }
        }

        // prepare the preference list for the next OpuS or FairRide run

        for (int i=0;i<userList.size();i++){
            prefFileList.add(i,new ArrayList<>());
        }

        int i = 0;
        for (String user : userPref.keySet()){
            prefFileList.get(i).addAll(userPref.get(user).keySet());
            i++;
        }

        File userLog = new File(currentDirectory+"/alluxio-gtcs/python/prefs.txt");
        try {
            FileOutputStream fop = new FileOutputStream(userLog,false);
            OutputStreamWriter writer = new OutputStreamWriter(fop);
            writer.write(String.valueOf(QUOTA*clientList.size())+'\n');
            for (String user: userPref.keySet()){
                int k = 0;
                for (Double pref : userPref.get(user).values()){
                    if (k != userPref.get(user).values().size()-1)
                        writer.write(pref.toString() + ", " );
                    else
                        writer.write(pref.toString() + "\n");
                    k++;
                }
            }
            writer.flush();
            writer.close();
            fop.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            freeAll();
            FileOutputStream f = new FileOutputStream(log,true);
            OutputStreamWriter w = new OutputStreamWriter(f);
            w.write("OpuS: \n");
            w.flush();
            LOG.info("Start OpuS");
            OpuSComparasion();


            freeAll();
            w.write("FairRide: \n");
            w.flush();
            LOG.info("Start FairRide");
            FairRideComparison();

            w.write("\n");
            w.flush();
            w.close();
            freeAll();
            for (String file:fileList.keySet()){
                fileList.replace(file,false);
            }
            for (String file:cacheList.keySet()){
                cacheList.replace(file,false);
            }
        } catch (FileDoesNotExistException | InvalidPathException |
                UnexpectedAlluxioException | AccessControlException | IOException e) {
            e.printStackTrace();
        }

    }

    private void HitRatio() throws AlluxioStatusException {
        double hit=0,access=0;
        for (String user : clientList.keySet()){
            Map<String,Integer> accessList;
            accessList=clientList.get(user).experimental_access(userPref.get(user));
            double h=0,acc=0;
            for (String file : accessList.keySet()){
                if (cacheList.get(file)) {
                    hit += accessList.get(file);
                    h += accessList.get(file);
                }
                acc += accessList.get(file);
                access += accessList.get(file);
            }
        }
        try {
            FileOutputStream fop = new FileOutputStream(log,true);
            OutputStreamWriter writer = new OutputStreamWriter(fop);
            writer.write("the experimental hit ratio is " + hit/access+"\n");
            writer.flush();
            writer.close();
            fop.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void Efficiency(int QUOTA) {
        int cacheNum = userList.size() * QUOTA;
        //
        // Calculate the optimized utility
        //
        for (String u : userPref.keySet()) {
            double efficiency = 0;
            for (String f : cacheList.keySet()) {
                try{
                    if (cacheList.get(f) && userPref.get(u).containsKey(f)
                            && userPref.get(u).get(f)!=null && !userPref.isEmpty()) {
                        efficiency += userPref.get(u).get(f);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
            if (utilList.keySet().contains(u)) {
                utilList.replace(u, efficiency);
            } else {
                utilList.put(u, efficiency);
            }
        }
        double sum = 0;
        for (double value : utilList.values()) {
            sum += value;
        }
        //
        // Calculate current utility
        //
        Map<String, Double> filePref = new HashMap<>();
        for (String f:cacheList.keySet()){
            double e = 0;
            for (String u:userPref.keySet()){
                if(userPref.get(u).get(f)!=null){
                    e +=userPref.get(u).get(f);
                }
            }
            filePref.put(f, e);
        }
        filePref = sortByValue(filePref);
        int count=0;
        double s=0;
        for (String key : filePref.keySet()){
            s+=filePref.get(key);
            if(count==cacheNum-1) break;
            count++;
        }
        try {
            FileOutputStream fop = new FileOutputStream(log,true);
            OutputStreamWriter writer = new OutputStreamWriter(fop);
            writer.write("Efficiency: "+s/sum+"\n");
            writer.flush();
            writer.close();
            fop.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.info("Efficiency: " + s/sum);
    }

    private void Access() throws AlluxioStatusException {
        ArrayList<Pair<Double,Long>> accessList = new ArrayList<>();
        for (GameSystemClient client:clientList.values()) {
            accessList.add(client.access("Game"));
        }
        double sum1=0;
        long sum2=0;
        for (int i=0;i<accessList.size();i++){
            sum1+=accessList.get(i).getFirst();
            sum2+=accessList.get(i).getSecond();
        }
        double ratio = sum1/accessList.size();
        long time = sum2/accessList.size();
        try {
            FileOutputStream fop = new FileOutputStream(log,true);
            OutputStreamWriter writer = new OutputStreamWriter(fop);
            writer.write("Actual Hit Ratio: " + ratio + "\n");
            writer.write("Average Access Latency: " + time + " ms\n");
            writer.flush();
            writer.close();
            fop.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.info("the actual hit ratio for game is " + ratio);
        LOG.info("the average overall access time for game is " + time + " ms");
    }

    private void Access_OpuS() throws IOException {
        ArrayList<Pair<Double,Long>> accessList = new ArrayList<>();
        for (GameSystemClient client:clientList.values()){
            accessList.add(client.access("OpuS"));
        }
        double sum1 = 0;
        double sum2 = 0;
        DataInputStream f = new DataInputStream(new FileInputStream(currentDirectory + "/alluxio-gtcs/python/factor_opus.txt"));
        Scanner sc = new Scanner(f);
        String str = sc.nextLine().replaceAll("\\s","");
        String[] values = new String[userList.size()];
        int i = 0;
        for (int j=0;j<userList.size();j++){
            values[j]="";
        }
        for (char c: str.toCharArray()){
            if(c==',') {
                i++;
            }else{
                values[i]+=c;
            }
        }
        long sum3=0L, time=0L;
        for (Pair<Double, Long> anAccessList : accessList) {
            Double factor = Double.parseDouble(values[accessList.indexOf(anAccessList)]);
            sum1 += anAccessList.getFirst() * factor;
            sum2 += anAccessList.getFirst();
            sum3 += anAccessList.getSecond();
        }
        double ratio1 = sum1/accessList.size();
        double ratio2 = sum2/accessList.size();
        time = sum3/accessList.size();
        try {
            FileOutputStream fop = new FileOutputStream(log,true);
            OutputStreamWriter writer = new OutputStreamWriter(fop);
            writer.write("Factorized Hit Ratio: " + ratio1 + "\n");
            writer.write("Normal Hit Ratio: " + ratio2 + "\n");
            writer.write("Average Access Latency: " + time + " ms\n");
            writer.flush();
            writer.close();
            fop.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.info("the factorized hit ratio for OpuS is "+ ratio1);
        LOG.info("the normal hit ratio for OpuS is "+ ratio2);
        LOG.info("the average overall access time for OpuS is " + time + " ms");
        f.close();
    }

    private void Access_FairRide() throws IOException {
        ArrayList<Pair<Double, Long>> accessList = new ArrayList<>();
        DataInputStream f = new DataInputStream(new FileInputStream(currentDirectory + "/alluxio-gtcs/python/factor_fairride.txt"));
        Scanner sc = new Scanner(f);
        for (GameSystemClient client:clientList.values()){
            ArrayList<Double> factorList = new ArrayList<>();
            String str = sc.nextLine().replaceAll("\\s","");
            String[] values = new String[fileList.size()];
            int i = 0;
            for (int j=0;j<fileList.size();j++){
                values[j]="";
            }
            for (char c: str.toCharArray()){
                if(c==',') {
                    i++;
                }else{
                    values[i]+=c;
                }
            }
            LOG.info(Arrays.toString(values));
            for (String s:values){
                if(!s.isEmpty()){
                    Double d = Double.parseDouble(s);
                    factorList.add(d);
                }
            }
            accessList.add(client.accessFairRide(factorList));
        }
        double sum1 = 0;
        long sum2 = 0;
        for (Pair<Double,Long> access : accessList) {
            sum1 += access.getFirst();
            sum2 += access.getSecond();
        }
        double ratio = sum1/accessList.size();
        long time = sum2/accessList.size();
        try {
            FileOutputStream fop = new FileOutputStream(log,true);
            OutputStreamWriter writer = new OutputStreamWriter(fop);
            writer.write("Factorized Hit Ratio: " + ratio + "\n");
            writer.write("Average Access Latency: " + time + " ms\n");
            writer.flush();
            writer.close();
            fop.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.info("the factorized hit ratio for FairRide is "+ ratio);
        LOG.info("the average overall access time for FairRide is " + time + " ms");
        f.close();
    }


    /**
     * Sort the Map by descending order according to the values
     */
    private <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {

        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());
        for (int i = 0; i < Math.floor(list.size() / 2) ; i++){
            Map.Entry<K, V> temp;
            temp = list.get(i);
            list.set(i,list.get(list.size()-i-1));
            list.set(list.size()-i-1,temp);
        }
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /** timer task to start GTC*/
    public class timer extends TimerTask {
        @Override
        public void run() {
                try {
                    LOG.info("Start GTC");
                    gameTheoreticalCommunication();
                } catch (AlluxioStatusException e) {
                    e.printStackTrace();
                }

            }
        }

    private void freeAll() throws FileDoesNotExistException, InvalidPathException,
            AccessControlException, UnexpectedAlluxioException, IOException {
        for (String file : cacheList.keySet()){
            if (cacheList.get(file)){
                fileSystemMaster.free(new AlluxioURI(file), FreeOptions.defaults());
                LOG.info("Free Process Complete, uri: " + file);
            }
        }
        LOG.info("FreeAll Process Complete" );
    }

}
