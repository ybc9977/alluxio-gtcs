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
import alluxio.master.file.options.FreeOptions;
import alluxio.thrift.AlluxioService;
import alluxio.thrift.GameSystemClientMasterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

/**
 *  created by byangak on 28/06/2018
 */

public final class GameSystemMaster extends AbstractMasterClient {

    private static final Logger LOG = LoggerFactory.getLogger(GameSystemMaster.class);

    private GameSystemClientMasterService.Client mClient = null;

    private FileSystemMaster mFileSystemMaster;

    public timer t = new timer();

    public GameSystemMaster(MasterClientConfig conf) {
        super(conf);
    }

    void addfile(String path){
        GameSystemMasterListMaintainer.addfile(path,false);
    }

    @Override
    protected AlluxioService.Client getClient() {
        return mClient;
    }

    @Override
    protected String getServiceName() {
        return Constants.FILE_SYSTEM_MASTER_CLIENT_SERVICE_NAME;
    }

    @Override
    protected long getServiceVersion() {
        return Constants.FILE_SYSTEM_MASTER_CLIENT_SERVICE_VERSION;
    }

    private synchronized List<String> checkCacheChange(Map<String, Boolean> fileList) throws AlluxioStatusException {
        return retryRPC(() -> mClient.checkCacheChange(fileList).cachingList, "CheckCacheChange");
    }

    public synchronized void gameTheoreticalCommunication() throws AlluxioStatusException {
        ArrayList<Pair<Long,Boolean>> userList = GameSystemMasterListMaintainer.getuser();
        while(true){
            int index = (int) (Math.random() * userList.size());
            Pair<Long, Boolean> user = userList.get(index);
            Map<String,Boolean> file_list = GameSystemMasterListMaintainer.getfile();
            List<String> cache_list;
            cache_list = checkCacheChange(file_list);
            if (cache_list!=null){
                for (String file_path: cache_list){
                    if(file_list.containsKey(file_path)){
                        file_list.replace(file_path,true);
                    }
                }
                userList.set(index,user.setSecond(true));
            }else{
                userList.set(index,user.setSecond(false));
            }
            GameSystemMasterListMaintainer.changefile(file_list);
            int count = 0;
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

        Map<String,Boolean> file_list = GameSystemMasterListMaintainer.getfile();
        for (String file:file_list.keySet()) {
            AlluxioURI uri = new AlluxioURI(file);
            if (file_list.get(file)) {
                try {
                    mFileSystemMaster.scheduleAsyncPersistence(uri);
                } catch (AlluxioException | UnavailableException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    mFileSystemMaster.free(uri, FreeOptions.defaults());
                } catch (FileDoesNotExistException | InvalidPathException |
                        AccessControlException | IOException | UnexpectedAlluxioException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class timer extends TimerTask {

        @Override
        public void run() {
            try {
                gameTheoreticalCommunication();
            } catch (AlluxioStatusException e) {
                e.printStackTrace();
            }
        }
    }
}
