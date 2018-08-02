package alluxio.master.gtcs;

import alluxio.collections.Pair;
import alluxio.exception.status.AlluxioStatusException;
import alluxio.master.file.FileSystemMaster;
import alluxio.thrift.*;
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

    private FileSystemMaster fileSystemMaster;

    public GameSystemMaster(FileSystemMaster defaultFileSystemMaster) {
        fileSystemMaster = defaultFileSystemMaster;
    }


    /** add file into fileList & cacheList */
    static void addfile(String path){
        fileList.put(path,false);
        cacheList.put(path,false);
        LOG.info("a file has been added successfully: " + path );
    }

//    //has not implemented yet
//    public static void deletefile(String path) {
//        if(fileList.containsKey(path)){
//            fileList.remove(path);
//        }else{
//            LOG.info("File not found");
//        }
//    }

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

//    //has not implemented yet
//    public static void deleteuser(Long userId) {
//        Pair<Long, Boolean> pair1 = new Pair<>(userId, false);
//        Pair<Long, Boolean> pair2 = new Pair<>(userId, true);
//        if(userList.contains(pair1)){
//            userList.remove(pair1);
//        }else if(userList.contains(pair2)){
//            userList.remove(pair2);
//        }else{
//            LOG.info("user "+ userId+ " not found");
//        }
//    }

    /** when external command is sent, change corresponding file status in the fileList & cacheList
     * currently has just implemented free file circumstance
     * @param path file path
     * @param isCached whether the file is cached or not */
    static void changeFileMode(final String path, final boolean isCached){
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
        InetSocketAddress mAddress = new InetSocketAddress(hostname, address.rpcPort);
        LOG.info("register a client with : " + mAddress.getAddress().toString());
        GameSystemClient client = new GameSystemClient(null,mAddress,userId);
        clientList.put(userId,client);
    }

    /** the main thread of game theoretical communication, run every 20 sec */
    private synchronized void gameTheoreticalCommunication() throws AlluxioStatusException {
        while(userList.size()!=0 && !fileList.isEmpty()){
            int index = (int) (Math.random() * userList.size());
            Pair<String, Boolean> user = userList.get(index);
            if(cacheMap.containsKey(user.getFirst()) && cacheMap.get(user.getFirst())!=null){
                for (String file: cacheMap.get(user.getFirst())){
                    if(fileList.get(file)){
                        fileList.replace(file,false);
                    }
                }
            }
            GameSystemClient client = clientList.get(user.getFirst());
            List<String> caching_list = client.checkCacheChange(fileList);
            LOG.info(String.valueOf("caching_list: "+caching_list+" for user "+user.getFirst()));
            // Check if caching_list stay the same with cacheMap, if so, do "else"
            boolean isChanged = false;
            if(cacheMap.get(user.getFirst())!=null){
                for (String key:caching_list){
                    if(!cacheMap.get(user.getFirst()).contains(key)){
                        isChanged = true;
                        break;
                    }
                }
            }else{
                isChanged = true;
            }
            if (isChanged) {
                user.setSecond(true);
                cacheMap.replace(user.getFirst(),caching_list);
            } else {
                user.setSecond(false);
            }
            for (String file_path : caching_list) {
                if (fileList.containsKey(file_path)) {
                    fileList.put(file_path,true);
                }
            }
            userList.set(index, user);
            int count = 0;
            LOG.info(String.valueOf("userList[(user,isChanged)]: "+userList));
            for (Pair<String,Boolean> p:userList) {
                if (p.getSecond()) {
                    count = 0;
                    break;
                }else{
                    count++;
                }
                if (count == userList.size()){
                    Pair<String,Boolean> U = userList.get((int) Math.floor(Math.random()*userList.size()));
                    GameSystemClient C = clientList.get(U.getFirst());
                    C.cacheIt(fileList,cacheList, fileSystemMaster);
                    return;
                }
            }
        }
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
