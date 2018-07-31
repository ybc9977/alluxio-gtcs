package alluxio.master.file;

import alluxio.AbstractClient;
import alluxio.AlluxioURI;
import alluxio.Constants;
import alluxio.exception.status.AlluxioStatusException;
import alluxio.master.MasterClientConfig;
import alluxio.thrift.AlluxioService;
import alluxio.thrift.GameSystemCacheService;
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

    private GameSystemFreeClient mGameSystemFreeClient = new GameSystemFreeClient(MasterClientConfig.defaults());

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

    public synchronized void cacheIt(Map<String,Boolean> fileList, Map<String,Boolean> cacheList){
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
                        mGameSystemFreeClient.free(uri, alluxio.client.file.options.FreeOptions.defaults());
                        LOG.info("Free Process Complete, uri: " + uri.getPath());
                        cacheList.replace(file,false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
