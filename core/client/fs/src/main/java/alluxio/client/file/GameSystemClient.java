package alluxio.client.file;

import java.io.IOException;
import java.util.*;

import alluxio.Constants;
import alluxio.Server;
import alluxio.thrift.GameSystemClientMasterService;
import org.apache.thrift.TProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  created by byangak on 28/06/2018
 */

public class GameSystemClient extends BaseFileSystem implements Server {

    private static final Logger LOG = LoggerFactory.getLogger(GameSystemClient.class);
    private static Map<String,ArrayList<String>> userList = new HashMap<>();

    /**
     * Constructs a new base file system.
     *
     * @param context file system context
     */
    public GameSystemClient(FileSystemContext context) {
        super(context);
    }

    private void setPrefList(Map<String,Boolean> fileList, String user){
        Map<String,Double> pref = new HashMap<>();
//        if (userList.get(user)==null || System.currentTimeMillis()%10000==0 || true ) {
            double p = 1.0;
            double p_now = 0;
            String lastKey = null;
            for (String path : fileList.keySet()) {
                p_now = p * Math.random();
                pref.put(path, p_now);
                p = p * (1 - p_now);
                lastKey = path;
                //prefList.set((int) (Math.random() * fileList.size()), path);
            }
            if (lastKey!=null) pref.replace(lastKey, p / (1 - p_now));
            pref = sortByValue(pref);
            LOG.info("randomized preference list for user " + user + " : " + pref.toString());
            ArrayList<String> mPrefList = new ArrayList<>(pref.keySet());
            userList.replace(user, mPrefList);
//        }
    }


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



        /**
     *
     * Check whether cache allocation need to change and how to
     *
     * @param fileList this time's fileList needed to be cached
     * @return cacheList or null
     */

     ArrayList<String> checkCacheChange(Map<String,Boolean> fileList, String user) {

        ArrayList<String> cachingList = new ArrayList<>();

        if (!userList.containsKey(user)){
            LOG.info("Client side server add user "+user);
            userList.put(user,null);
        }
        //String quota = WORKER_TIERED_STORE_LEVEL0_DIRS_QUOTA.getDefaultValue();
        //String size = USER_BLOCK_SIZE_BYTES_DEFAULT.getDefaultValue();

        setPrefList(fileList, user);
        LOG.info("userList(user[pref]):"+String.valueOf(userList));

        int cacheNum = 2;

        for(String path: userList.get(user)){
//            LOG.info("fileList:"+String.valueOf(fileList));
//            LOG.info("cacheNum >0 :"+String.valueOf(cacheNum));
//            LOG.info("fileList.containsKey(path) :"+String.valueOf(fileList.containsKey(path)));
//            LOG.info("!fileList.get(path) :"+String.valueOf(!fileList.get(path)));
            if (cacheNum >0 && fileList.containsKey(path) && !fileList.get(path)){
                cacheNum--;
                cachingList.add(path);
                fileList.replace(path,true);
//                LOG.info("hello world!");
            }else if (cacheNum <= 0){
                return cachingList;
            }
        }
        return cachingList;
    }

    @Override
    public Set<Class<? extends Server>> getDependencies() {
        return new HashSet<>();
    }

    @Override
    public String getName() {
        return Constants.GAME_SYSTEM_CLIENT_MASTER_SERVICE_NAME;
    }

    @Override
    public Map<String, TProcessor> getServices() {
        Map<String, TProcessor> services = new HashMap<>();
        services.put(Constants.GAME_SYSTEM_CLIENT_MASTER_SERVICE_NAME,
                new GameSystemClientMasterService.Processor<>(
                        new GameSystemClientMasterServiceHandler(this)));
        return services;
    }

    @Override
    public void start(Object options) throws IOException {
        LOG.info("Starting client server to reply for Master's require" );
    }

    @Override
    public void stop() throws IOException {
        LOG.info("Stopping client server to reply for Master's require");
    }

}
