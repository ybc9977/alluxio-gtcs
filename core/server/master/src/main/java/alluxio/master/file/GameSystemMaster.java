package alluxio.master.file;

import alluxio.AbstractMasterClient;
import alluxio.AlluxioURI;
import alluxio.Constants;
import alluxio.collections.Pair;
import alluxio.exception.status.AlluxioStatusException;
import alluxio.master.GameSystemMasterListMaintainer;
import alluxio.master.MasterClientConfig;
import alluxio.thrift.AlluxioService;
import alluxio.thrift.GameSystemClientMasterService;
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

    private Map<String,List<String>> cacheMap = new HashMap<>();

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

    private synchronized List<String> checkCacheChange(Map<String, Boolean> fileList, String user) throws AlluxioStatusException {
        return retryRPC(() -> mClient.checkCacheChange(fileList, user).cachingList, "CheckCacheChange");
    }

    public synchronized void gameTheoreticalCommunication() throws AlluxioStatusException {
        ArrayList<Pair<String,Boolean>> userList = GameSystemMasterListMaintainer.getUserList();
        while(userList.size()!=0 && !GameSystemMasterListMaintainer.getFileList().isEmpty()){
            
            int index = (int) (Math.random() * userList.size());
            Pair<String, Boolean> user = userList.get(index);
            Map<String,Boolean> file_list = GameSystemMasterListMaintainer.getFileList();
            List<String> caching_list = checkCacheChange(file_list, user.getFirst());
            LOG.info(String.valueOf("cacheMap: "+cacheMap));

            if (cacheMap.containsKey(user.getFirst()) && caching_list!=null) {

                if (!caching_list.equals(cacheMap.get(user.getFirst()))){
                    for (String file_path : caching_list) {
                        if (file_list.containsKey(file_path)) {
                            file_list.put(file_path,true);
                        }
                    }
                    GameSystemMasterListMaintainer.setFileList(file_list);
                    user.setSecond(true);
                    userList.set(index, user);
                    cacheMap.replace(user.getFirst(),caching_list);
                } else {
                    user.setSecond(false);
                    userList.set(index, user);
                }

            }else if (caching_list!=null){

                for (String file_path : caching_list) {
                    if (file_list.containsKey(file_path)) {
                        file_list.replace(file_path,true);
                    }
                }
                GameSystemMasterListMaintainer.setFileList(file_list);
                user.setSecond(true);
                userList.set(index, user);
                cacheMap.put(user.getFirst(),caching_list);

            }else{
                user.setSecond(false);
                userList.set(index, user);
                cacheMap.put(user.getFirst(),caching_list);
            }

            int count = 0;

            for (Pair<String,Boolean> p:userList) {
                if (p.getSecond()) {
                    count = 0;
                    break;
                }else{count++;}
                if (count == userList.size()){
                    cacheIt();
                    return;
                }
            }

        }
    }
    private synchronized void cacheIt(){
        Map<String,Boolean> fileList = GameSystemMasterListMaintainer.getFileList();
        Map<String,Boolean> cacheList = GameSystemMasterListMaintainer.getCacheList();
        LOG.info("fileList: "+String.valueOf(fileList));
        LOG.info("cacheList: "+String.valueOf(cacheList));

        for (String file:fileList.keySet()) {
            if(cacheList.containsKey(file)){
                if (fileList.get(file)!=cacheList.get(file)){
                    LOG.info("start the caching process");
                    AlluxioURI uri = new AlluxioURI(file);
                    if (fileList.get(file)) {
                        try {
                            mGameSystemCacheMaster.scheduleAsyncPersist(uri);
                            LOG.info("Persist Process Complete");
                        } catch (AlluxioStatusException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            mGameSystemCacheMaster.free(uri, alluxio.client.file.options.FreeOptions.defaults());
                            LOG.info("Free Process Complete");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    GameSystemMasterListMaintainer.setCacheList(fileList);
                }else{
                    LOG.info("fileList is the same as cacheList");
                }
            }else{
                LOG.warn("file "+ file +" is not found in the cacheList");
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
