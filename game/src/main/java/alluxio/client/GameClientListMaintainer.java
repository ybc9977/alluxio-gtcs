package alluxio.client;

import alluxio.master.GameMasterListMaintainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *  created by byangak on 01/07/2018
 */

public final class GameClientListMaintainer {

    private final static Logger LOG = LoggerFactory.getLogger(GameMasterListMaintainer.class);

    private static ArrayList<Long> prefList = new ArrayList<>();

    private static ArrayList<Long> cacheList = new ArrayList<>();

    private static Long userId = 0L;

    public GameClientListMaintainer(){}

    public void setUserId(Long user){
        userId = user;
    }
    public Long getUserId(){
        return userId;
    }

    public ArrayList<Long> getPrefList(Map<Long,Boolean> fileList){

        if (System.currentTimeMillis()%10000 == 0){
            for(Long file: fileList.keySet()){
                prefList.set((int) (Math.random() * fileList.size()), file);
                LOG.info("randomized preference list for user "+ userId);
            }
        }
        return prefList;
    }

    public void changeCacheList(ArrayList<Long> mCacheList){
        cacheList =  mCacheList;
        LOG.info("CacheList changed for user "+userId);
    }

    public ArrayList<Long> getCacheList(){
        return cacheList;
    }

}
