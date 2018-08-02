package alluxio.client.gtcs;

import java.util.*;
import alluxio.Server;
import alluxio.client.file.BaseFileSystem;
import alluxio.client.file.FileSystemContext;
import alluxio.thrift.ClientNetAddress;
import alluxio.thrift.GameSystemCacheService;
import com.google.common.base.Preconditions;
import org.apache.commons.math3.distribution.ZipfDistribution;
import org.apache.thrift.TProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  created by byangak on 28/06/2018
 */

public class GameSystemServer extends BaseFileSystem implements Server<ClientNetAddress> {
    private static final Logger LOG = LoggerFactory.getLogger(GameSystemServer.class);

    private ArrayList<String> prefList;
    private String mUserId;
    public ClientNetAddress mAddress;
//    private final ExecutorService mExecutorService;

    /**
     * Constructs a new base file system.
     *
     * @param context file system context
     */
    public GameSystemServer(FileSystemContext context, String userId) {
        super(context);
        mUserId = userId;
//        mExecutorService = Executors.newFixedThreadPool(1,
//                ThreadFactoryUtils.build("game-system-client-%d", true));
    }

    public String getUserId(){
        return mUserId;
    }

    private void setPrefList(Map<String,Boolean> fileList){
        ArrayList<String> list = new ArrayList<>(fileList.keySet());
        Collections.shuffle(list, new Random());
        Map<String,Double> pref = new HashMap<>();
        for (String aList : list) {
            pref.put(aList, 0.0);
        }
        ZipfDistribution zd = new ZipfDistribution(fileList.size(),1.05);
        int count = 1;
        for (String path : fileList.keySet()) {
            pref.put(path, zd.probability(count));
            count++;
        }
        LOG.info(pref.toString());
        prefList= new ArrayList<>(pref.keySet());

    }

    /**
     * Sort the Map by ascending order according to the values
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

    /**
     *
     * Check whether cache allocation need to change and how to
     *
     * @param fileList this time's fileList needed to be cached
     * @return cacheList or null
     */

     ArrayList<String> checkCacheChange(Map<String,Boolean> fileList) {

        ArrayList<String> cachingList = new ArrayList<>();
//        String quota = WORKER_TIERED_STORE_LEVEL0_DIRS_QUOTA.getDefaultValue();
//        String size = USER_BLOCK_SIZE_BYTES_DEFAULT.getDefaultValue();
        setPrefList(fileList);
        LOG.info("randomized preference list for user " + mUserId + " : " + prefList.toString());

        int quota = 2;

        for(String path: prefList){
            if (quota >0 && fileList.containsKey(path) && !fileList.get(path)){
                quota--;
                cachingList.add(path);
                fileList.replace(path,true);
            }else if (quota <= 0){
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
        return "Client "+mUserId;
    }

    @Override
    public Map<String, TProcessor> getServices() {
        Map<String, TProcessor> services = new HashMap<>();
        services.put("Client "+mUserId,
                new GameSystemCacheService.Processor<>(
                        new GameSystemServiceHandler(this)));
        return services;
    }

    @Override
    public void start(ClientNetAddress address) {
         mAddress = address;
         Preconditions.checkNotNull(mAddress, "mAddress");
         LOG.info("Starting client server to reply for Master's require" );
    }


    @Override
    public void stop() {
        LOG.info("Stopping client server to reply for Master's require");
    }

}