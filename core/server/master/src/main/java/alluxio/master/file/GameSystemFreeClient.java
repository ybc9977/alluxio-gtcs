package alluxio.master.file;

import alluxio.AbstractMasterClient;
import alluxio.AlluxioURI;
import alluxio.Constants;
import alluxio.client.file.options.FreeOptions;
import alluxio.exception.status.AlluxioStatusException;
import alluxio.master.MasterClientConfig;
import alluxio.thrift.AlluxioService;
import alluxio.thrift.GameSystemFreeService;

/**
 *  created by byangak on 18/07/2018
 *
 *  in order to achieve free option, which is used by GameSystemClient, call master by master
 */

public class GameSystemFreeClient extends AbstractMasterClient {

    private GameSystemFreeService.Client mClient = null;

    public GameSystemFreeClient(MasterClientConfig conf) {
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
        mClient = new GameSystemFreeService.Client(mProtocol);
    }

    public synchronized void free(final AlluxioURI path, final FreeOptions options)
            throws AlluxioStatusException {
        retryRPC(() -> mClient.free(path.getPath(),false , options.toThrift()), "Free");
    }

}
