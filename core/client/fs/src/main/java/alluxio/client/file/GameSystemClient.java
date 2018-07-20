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

public class GameSystemClient extends BaseFileSystem implements Server<Long> {

    private static final Logger LOG = LoggerFactory.getLogger(GameSystemClient.class);
    private ArrayList<String> mPrefList = new ArrayList<>();
    private Map<String,Double> pref = new HashMap<>();
    public Long mUserId;

    /**
     * Constructs a new base file system.
     *
     * @param context file system context
     */
    public GameSystemClient(FileSystemContext context) {

        super(context);
        mUserId = 0L;
    }

    public GameSystemClient(FileSystemContext context, Long userId) {

        super(context);
        mUserId = userId;

    }

    private void setPrefList(Map<String,Boolean> fileList){

        double p = 1.0;
        for(String path: fileList.keySet()){
            double p_now = p * Math.random();
            pref.put(path,p_now);
            p = p * (1-p_now);
            //prefList.set((int) (Math.random() * fileList.size()), path);
        }

        LOG.info("randomized preference list for user "+ mUserId );
        pref = sortByValue(pref);

        LOG.info(pref.toString());
        mPrefList.addAll(pref.keySet());
    }


    private <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {

        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());

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

        //String quota = WORKER_TIERED_STORE_LEVEL0_DIRS_QUOTA.getDefaultValue();
        //String size = USER_BLOCK_SIZE_BYTES_DEFAULT.getDefaultValue();

        setPrefList(fileList);

        float cacheNum = 2;

        for(String path: mPrefList){
            if (cacheNum >0 && fileList.containsKey(path) && !fileList.get(path)){
                cacheNum--;
                cachingList.add(path);
                fileList.replace(path,true);
            }else if (cacheNum <= 0){
                    return cachingList;
            }
        }

        return null;

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
    public void start(Long userId) throws IOException {
        mUserId = userId;
        LOG.info("Starting user " + mUserId + "'s server to reply for Master's require" );
    }

    @Override
    public void stop() throws IOException {
        LOG.info("Stopping user " + mUserId + "'s server to reply for Master's require");
    }

}
