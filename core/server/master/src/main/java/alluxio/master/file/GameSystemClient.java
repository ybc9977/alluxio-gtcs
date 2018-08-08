package alluxio.master.file;

import alluxio.AbstractClient;
import alluxio.AlluxioURI;
import alluxio.Constants;
import alluxio.exception.AccessControlException;
import alluxio.exception.FileDoesNotExistException;
import alluxio.exception.InvalidPathException;
import alluxio.exception.UnexpectedAlluxioException;
import alluxio.exception.status.AlluxioStatusException;
import alluxio.master.file.options.FreeOptions;
import alluxio.thrift.AlluxioService;
import alluxio.thrift.GameSystemCacheService;
import alluxio.thrift.GetPrefTOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

/**
 *  created by byangak on 30/07/2018
 *
 *  Game System Master side client class, one instance to communicate with certain Game System Server
 */

public class GameSystemClient extends AbstractClient {
    private static final Logger LOG = LoggerFactory.getLogger(GameSystemMaster.class);

    private GameSystemCacheService.Client mClient = null;

    private String mUserId;

    GameSystemClient(Subject subject, InetSocketAddress address, String userId) {
        super(subject, address);
        mUserId = userId;
    }

    @Override
    protected AlluxioService.Client getClient() {
        return mClient;
    }

    @Override
    protected String getServiceName() {
        return "Client "+mUserId;
    }

    @Override
    protected long getServiceVersion() {
        return Constants.GAME_SYSTEM_SERVICE_VERSION;
    }

    @Override
    protected void afterConnect() {
        mClient = new GameSystemCacheService.Client(mProtocol);
    }

    /** a remote procedure to call in client side server
     * @param fileList a map contains filePath & isCached */
    public synchronized List<String> checkCacheChange(Map<String, Boolean> fileList) throws AlluxioStatusException {
        return retryRPC(() -> mClient.checkCacheChange(fileList).cachingList, "CheckCacheChange");
    }

    public Map<String,Double> getPref() throws AlluxioStatusException {
        return retryRPC(() -> mClient.getPref(new GetPrefTOptions()).pref, "GetPref");
    }

    public Map<String, Integer> access(Map<String,Double> prefList) throws AlluxioStatusException {
        return retryRPC(() -> mClient.access(prefList).access, "Access");
    }

    public synchronized void cacheIt(Map<String,Boolean> fileList, Map<String,Boolean> cacheList, FileSystemMaster fsMaster){
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
                        fsMaster.free(uri,FreeOptions.defaults());
                        LOG.info("Free Process Complete, uri: " + uri.getPath());
                        cacheList.replace(file,false);
                    } catch (IOException | AccessControlException | FileDoesNotExistException | InvalidPathException | UnexpectedAlluxioException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
