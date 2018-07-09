package alluxio.client.file;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import alluxio.AlluxioURI;
import alluxio.client.GameClientListMaintainer;
import alluxio.exception.AlluxioException;
import alluxio.master.GameMasterListMaintainer;
import alluxio.thrift.CheckCacheChangeTOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  created by byangak on 28/06/2018
 */

public class GameFileSystem extends BaseFileSystem {

    private static final Logger LOG = LoggerFactory.getLogger(GameFileSystem.class);
    private ArrayList<Long> prefList = new ArrayList<Long>();
    private float cacheNum = 0;
    private GameClientListMaintainer mGameClientListMaintainer = new GameClientListMaintainer();


    /**
     * Constructs a new base file system.
     *
     * @param context file system context
     */
    protected GameFileSystem(FileSystemContext context) {

        super(context);
    }

    protected GameFileSystem(FileSystemContext context, Long userId) {

        super(context);

//        Random random = new Random();
//
//        userId = random.nextLong();

        mGameClientListMaintainer.setUserId(userId);

    }

    private void setPrefList(Map<Long,Boolean> fileList){

        prefList = mGameClientListMaintainer.getPrefList(fileList);
    }

    /**
     *
     * Check whether cache allocation need to change and how to
     *
     * @param fileList this time's fileList needed to be cached
     * @param options whether need to give a choice or not
     * @return cacheList or null
     */

    public ArrayList<Long> checkCacheChange(Map<Long,Boolean> fileList, CheckCacheChangeTOptions options) throws IOException, AlluxioException {

        if (options.getUserId()==mGameClientListMaintainer.getUserId()){
            if (!options.isImplement()){
                ArrayList<Long> cacheList = new ArrayList<>();

                //String quota = WORKER_TIERED_STORE_LEVEL0_DIRS_QUOTA.getDefaultValue();
                //String size = USER_BLOCK_SIZE_BYTES_DEFAULT.getDefaultValue();

                setPrefList(fileList);

                cacheNum = 2; // default setting

                for(Long file: prefList){
                    if (cacheNum>0 && fileList.containsKey(file) && !fileList.get(file)){
                        cacheNum--;
                        cacheList.add(file);
                    }else if (cacheNum <= 0){
                        mGameClientListMaintainer.changeCacheList(cacheList);
                        return cacheList;
                    }
                }
            }else{
                ArrayList<Long> cacheList = mGameClientListMaintainer.getCacheList();
                //to implement file caching process here
                AlluxioURI path = new AlluxioURI(".");
                createFile(path);
            }
        }
        return null;
    }
}
