package alluxio.master.file;

import alluxio.collections.Pair;
import alluxio.exception.status.AlluxioStatusException;
import alluxio.thrift.ClientNetAddress;
import alluxio.thrift.RegisterUserTOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;

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
    private static Map<String,Boolean> fileList = new HashMap<>();

    /** FileList which is settled and always representing the realistic circumstance */
    private static Map<String,Boolean> cacheList = new HashMap<>();

    /** Each user is a Pair contains userId(Long) and isCachingOptionChanged(Boolean) */
    private static ArrayList<Pair<String,Boolean>> userList = new ArrayList<>();

    private static Map<String,Double> utilList = new HashMap<>();

    private static Map<String, Map<String, Double>> userPref = new HashMap<>();

    private FileSystemMaster fileSystemMaster;

    private static double start_time;

    public GameSystemMaster(FileSystemMaster defaultFileSystemMaster) {
        fileSystemMaster = defaultFileSystemMaster;
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

    /** the main thread of game theoretical communication, run every 20 sec */
    private synchronized void gameTheoreticalCommunication() throws AlluxioStatusException {
        int QUOTA = 300 / clientList.size();

        start_time =System.currentTimeMillis();
        int poll_iter = 0;
        Collections.shuffle(userList);
        while(userList.size()!=0 && !fileList.isEmpty()){
            double iter_start = System.currentTimeMillis();
            int userPos = poll_iter % userList.size();
            Pair<String, Boolean> user = userList.get(userPos);
            String userID = user.getFirst();

            // file list pretreatment
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

//            LOG.info("fileList: " + fileList.toString());

            LOG.info(String.valueOf("userList[(user,isChanged)]: "+userList));

            int count = 0;
            poll_iter++;
            for (Pair<String,Boolean> p:userList) {
                if (p.getSecond()) {
                    break;
                }else{
                    count++;
                }
                if (count == userList.size()){
                    LOG.info("Total time cost(ms): "+(System.currentTimeMillis()-start_time));
                    for (GameSystemClient clt : clientList.values()){
                        clt.reset();
                    }
                    Pair<String,Boolean> U = userList.get((int) Math.floor(Math.random()*userList.size()));
                    GameSystemClient C = clientList.get(U.getFirst());
                    C.cacheIt(fileList,cacheList,fileSystemMaster);
                    LOG.info("Equilibrium established");
                    LOG.info("Total iteration: "+poll_iter);
                    Efficiency(QUOTA);
                    HitRatio();
                    return;
                }
            }
            LOG.info("Iter num: " + poll_iter + " Time cost : " + (System.currentTimeMillis()-iter_start));
            if (poll_iter%userList.size()==0){
                Collections.shuffle(userList);
            }
        }
    }

    private void HitRatio() throws AlluxioStatusException {
        double hit=0,access=0;
        for (String user : clientList.keySet()){
            Map<String,Integer> accessList;
            accessList=clientList.get(user).access(userPref.get(user));
//            LOG.info("user "+ user +"'s accessList "+accessList.toString());
            double h=0,acc=0;
            for (String file : accessList.keySet()){
                if (cacheList.get(file)) {
                    hit += accessList.get(file);
                    h += accessList.get(file);
                }
                acc += accessList.get(file);
                access += accessList.get(file);
            }
//            LOG.info("user " + user+"'s hit ratio is " + h/acc);
        }
        LOG.info("the overall hit ratio is " + hit/access);
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
        LOG.info("Efficiency: " + s/sum);
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
                gameTheoreticalCommunication();
                LOG.info("Start GTC");
            } catch (AlluxioStatusException e) {
                e.printStackTrace();
            }
        }
    }
}
