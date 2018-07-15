package alluxio.master.file;

import alluxio.AbstractMasterClient;
import alluxio.AlluxioURI;
import alluxio.Constants;
import alluxio.collections.Pair;
import alluxio.exception.*;
import alluxio.exception.status.AlluxioStatusException;
import alluxio.exception.status.UnavailableException;
import alluxio.master.GameSystemMasterListMaintainer;
import alluxio.master.MasterClientConfig;
import alluxio.master.MasterContext;
import alluxio.master.MasterRegistry;
import alluxio.master.block.DefaultBlockMaster;
import alluxio.master.file.options.FreeOptions;
import alluxio.thrift.AlluxioService;
import alluxio.thrift.GameSystemClientMasterService;
import alluxio.thrift.ScheduleAsyncPersistenceTOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 *  created by byangak on 28/06/2018
 */

public final class GameSystemMaster extends AbstractMasterClient {

    private static final Logger LOG = LoggerFactory.getLogger(GameSystemMaster.class);

    private GameSystemClientMasterService.Client mClient = null;

    public timer t = new timer();

    public GameSystemMaster(MasterClientConfig conf) {
        super(conf);
    }

    private GameSystemCacheMaster mGameSystemCacheMaster = new GameSystemCacheMaster(MasterClientConfig.defaults());

    @Override
    protected AlluxioService.Client getClient() {
        return mClient;
    }

    @Override
    protected String getServiceName() {
        return Constants.GAME_SYSTEM_CLIENT_MASTER_SERVICE_NAME;
    }

    @Override
    protected long getServiceVersion() {
        return Constants.GAME_SYSTEM_CLIENT_MASTER_SERVICE_VERSION;
    }

    @Override
    protected void afterConnect() {
        mClient = new GameSystemClientMasterService.Client(mProtocol);
    }
    private synchronized List<String> checkCacheChange(Map<String, Boolean> fileList) throws AlluxioStatusException {
        return retryRPC(() -> mClient.checkCacheChange(fileList).cachingList, "CheckCacheChange");
    }

    public synchronized void gameTheoreticalCommunication() throws AlluxioStatusException {
        ArrayList<Pair<Long,Boolean>> userList = GameSystemMasterListMaintainer.getuser();
        Map<Long,List<String>> cacheMap = new HashMap<>();
        while(userList.size()!=0){
            int index = (int) (Math.random() * userList.size());
            Pair<Long, Boolean> user = userList.get(index);
            Map<String,Boolean> file_list = GameSystemMasterListMaintainer.getfilebefore();
            List<String> cache_list = checkCacheChange(file_list);
            LOG.info(String.valueOf("cacheMap: "+cacheMap));
            LOG.info(String.valueOf("cache_list: "+cache_list));
            LOG.info(String.valueOf("MAP_LIST: "+cacheMap.get(user.getFirst())));
            //LOG.info(String.valueOf(cache_list.equals(cacheMap.get(user.getFirst()))));
            if (cacheMap.containsKey(user.getFirst())) {
                if (!cache_list.equals(cacheMap.get(user.getFirst()))){
                    for (String file_path : cache_list) {
                        if (file_list.containsKey(file_path)) {
                            file_list.replace(file_path, true);
                        }
                    }
                    user.setSecond(true);
                    userList.set(index, user);
                    cacheMap.replace(user.getFirst(),cache_list);
                } else {
                    user.setSecond(false);
                    userList.set(index, user);
                }
            }else if (cache_list!=null){
                for (String file_path : cache_list) {
                    if (file_list.containsKey(file_path)) {
                        file_list.replace(file_path, true);
                    }
                }
                user.setSecond(true);
                userList.set(index, user);
                cacheMap.put(user.getFirst(),cache_list);
            }else{
                user.setSecond(false);
                userList.set(index, user);
                cacheMap.put(user.getFirst(),cache_list);
            }
            GameSystemMasterListMaintainer.changefile(file_list);

            int count = 0;
            LOG.info(String.valueOf(userList));
            for (Pair<Long,Boolean> p:userList) {
                if (p.getSecond()) {
                    count = 0;
                    break;
                }else{
                    count++;
                }
                if (count == userList.size()){
                    cacheIt();
                    return;
                }
            }
        }
    }
    private synchronized void cacheIt(){

        Map<String,Boolean> file_list_before = GameSystemMasterListMaintainer.getfilebefore();
        Map<String,Boolean> file_list_after = GameSystemMasterListMaintainer.getfileafter();
        LOG.info("start the caching process");
        for (String file:file_list_before.keySet()) {
            AlluxioURI uri = new AlluxioURI(file);
            if (file_list_before.get(file)!=file_list_after.get(file)){
                if (file_list_after.get(file)) {
                    try {
                        mGameSystemCacheMaster.scheduleAsyncPersist(uri);
                        LOG.info("persist complete");
                    } catch (AlluxioStatusException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        mGameSystemCacheMaster.free(uri, alluxio.client.file.options.FreeOptions.defaults());
                        LOG.info("free complete");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                GameSystemMasterListMaintainer.setFileListBefore(file_list_after);
            }else{
                LOG.info("cache lists are the same, didn't change caching list");
            }
        }
    }

    public class timer extends TimerTask {

        @Override
        public void run() {
            LOG.info("Start GTC");
            try {
                gameTheoreticalCommunication();
            } catch (AlluxioStatusException e) {
                e.printStackTrace();
            }
        }
    }
}
