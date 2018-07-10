package alluxio.client.file;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import alluxio.client.GameSystemClientListMaintainer;
import alluxio.exception.AlluxioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  created by byangak on 28/06/2018
 */

public class GameSystemClient extends BaseFileSystem {

    private static final Logger LOG = LoggerFactory.getLogger(GameSystemClient.class);
    private ArrayList<String> prefList = new ArrayList<>();
    private float cacheNum = 0;
    private GameSystemClientListMaintainer mGameSystemClientListMaintainer = new GameSystemClientListMaintainer();


    /**
     * Constructs a new base file system.
     *
     * @param context file system context
     */
    protected GameSystemClient(FileSystemContext context) {

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
            }else if (cacheNum <= 0){
                mGameSystemClientListMaintainer.changeCacheList(cacheList);
                return cacheList;
            }
        }
        return null;
    }
}
