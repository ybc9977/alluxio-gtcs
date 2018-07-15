package alluxio.client.file;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import alluxio.Client;
import alluxio.Constants;
import alluxio.Server;
import alluxio.client.GameSystemClientListMaintainer;
import alluxio.thrift.GameSystemClientMasterService;
import alluxio.util.ThreadFactoryUtils;
import com.google.common.base.Preconditions;
import org.apache.thrift.TProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  created by byangak on 28/06/2018
 */

public class GameSystemClient extends BaseFileSystem implements Server<Long> {

    private static final Logger LOG = LoggerFactory.getLogger(GameSystemClient.class);
    private ArrayList<String> prefList = new ArrayList<>();
    private float cacheNum = 0;
    private GameSystemClientListMaintainer mGameSystemClientListMaintainer = new GameSystemClientListMaintainer();
    private Long mUserId;

    /**
     * Constructs a new base file system.
     *
     * @param context file system context
     */
    public GameSystemClient(FileSystemContext context) {

        super(context);
    }

    protected GameSystemClient(FileSystemContext context, Long userId) {

        super(context);

//        Random random = new Random();
//
//        userId = random.nextLong();

        mGameSystemClientListMaintainer.setUserId(userId);

    }

    private void setPrefList(Map<String,Boolean> fileList){

        prefList = mGameSystemClientListMaintainer.getPrefList(fileList);
    }

    /**
     *
     * Check whether cache allocation need to change and how to
     *
     * @param fileList this time's fileList needed to be cached
     * @return cacheList or null
     */

     ArrayList<String> checkCacheChange(Map<String,Boolean> fileList) {
        ArrayList<String> cacheList = new ArrayList<>();

        //String quota = WORKER_TIERED_STORE_LEVEL0_DIRS_QUOTA.getDefaultValue();
        //String size = USER_BLOCK_SIZE_BYTES_DEFAULT.getDefaultValue();

        setPrefList(fileList);

        cacheNum = 2; // default setting

        for(String path: prefList){
            if (cacheNum>0 && fileList.containsKey(path) && !fileList.get(path)){
                cacheNum--;
                cacheList.add(path);
                fileList.replace(path,true);
            }else if (cacheNum <= 0){
                //if(mGameSystemClientListMaintainer.changeCacheList(cacheList)){
                    return cacheList;
                //}else {
                //    return null;
                //}

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
