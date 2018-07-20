package alluxio.master.file;

import alluxio.AbstractMasterClient;
import alluxio.AlluxioURI;
import alluxio.Constants;
import alluxio.client.file.options.FreeOptions;
import alluxio.exception.status.AlluxioStatusException;
import alluxio.master.MasterClientConfig;
import alluxio.thrift.AlluxioService;
import alluxio.thrift.GameSystemCacheService;
import alluxio.thrift.ScheduleAsyncPersistenceTOptions;

/**
 *  created by byangak on 18/07/2018
 */

public class GameSystemCacheMaster extends AbstractMasterClient {

    private GameSystemCacheService.Client mClient = null;

    public GameSystemCacheMaster(MasterClientConfig conf) {
        super(conf);
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

    @Override
    protected void afterConnect() {
        mClient = new GameSystemCacheService.Client(mProtocol);
    }

    public synchronized void scheduleAsyncPersist(final AlluxioURI path)
            throws AlluxioStatusException {
        retryRPC(() -> mClient.scheduleAsyncPersistence(path.getPath(),
                new ScheduleAsyncPersistenceTOptions()), "ScheduleAsyncPersist");
    }
    public synchronized void free(final AlluxioURI path, final FreeOptions options)
            throws AlluxioStatusException {
        retryRPC(() -> mClient.free(path.getPath(),false , options.toThrift()), "Free");
    }

}
