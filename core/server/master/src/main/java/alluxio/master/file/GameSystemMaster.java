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
        Map<String,List<String>> cacheMap = GameSystemMasterListMaintainer.getCacheMap();
        ArrayList<Pair<String,Boolean>> userList = GameSystemMasterListMaintainer.getUserList();
        Map<String,Boolean> file_list = GameSystemMasterListMaintainer.getFileList();
        while(userList.size()!=0 && !file_list.isEmpty()){

            int index = (int) (Math.random() * userList.size());
            Pair<String, Boolean> user = userList.get(index);
            if(cacheMap.containsKey(user.getFirst()) && cacheMap.get(user.getFirst())!=null){
                for (String file: cacheMap.get(user.getFirst())){
                    if(file_list.get(file)){
                        file_list.replace(file,false);
                    }
                }
            }

            List<String> caching_list = checkCacheChange(file_list, user.getFirst());
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
                if (file_list.containsKey(file_path)) {
                    file_list.put(file_path,true);
                }
            }


            userList.set(index, user);

            LOG.info("cacheMap: "+cacheMap);
            GameSystemMasterListMaintainer.setCacheMap(cacheMap);

            int count = 0;

            LOG.info(String.valueOf("userList([user,isChanged]): "+userList));

            for (Pair<String,Boolean> p:userList) {
                if (p.getSecond()) {
                    count = 0;
                    break;
                }else{
                    count++;
                }
                if (count == userList.size()){
                    cacheIt(file_list);
                    return;
                }
            }

        }
        GameSystemMasterListMaintainer.setFileList(file_list);
    }
    private synchronized void cacheIt(Map<String,Boolean> fileList ){
        Map<String,Boolean> cacheList = GameSystemMasterListMaintainer.getCacheList();
        for (String file:fileList.keySet()){
            AlluxioURI uri = new AlluxioURI(file);
            if(fileList.get(file)!=cacheList.get(file)){
                if(fileList.get(file)){
                    try {
                        retryRPC(() -> mClient.load(file), "Load");
                        LOG.info("Load Process Complete, uri: " + uri.getPath());
                        cacheList.replace(file,true);
                    } catch (AlluxioStatusException e) {
                        e.printStackTrace();
                    }
                }else{
                    try {
                        mGameSystemCacheMaster.free(uri, alluxio.client.file.options.FreeOptions.defaults());
                        LOG.info("Free Process Complete, uri: " + uri.getPath());
                        cacheList.replace(file,false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        GameSystemMasterListMaintainer.setFileList(fileList);
        GameSystemMasterListMaintainer.setCacheList(cacheList);
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
