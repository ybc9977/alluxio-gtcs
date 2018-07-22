package alluxio.client.file;

import alluxio.AlluxioURI;
import alluxio.Constants;
import alluxio.RpcUtils;
import alluxio.client.ReadType;
import alluxio.client.file.options.OpenFileOptions;
import alluxio.thrift.*;
import com.google.common.base.Preconditions;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;

/**
 *  created by byangak on 28/06/2018
 */

public class GameSystemClientMasterServiceHandler implements
        GameSystemClientMasterService.Iface {
    private static final Logger LOG =
            LoggerFactory.getLogger(GameSystemClientMasterServiceHandler.class);
    //
    private final GameSystemClient mGameSystemClient;

    /**
     * Creates a new instance of {@link GameSystemClientMasterServiceHandler}.
     *
     * @param gameSystemClient the {@link GameSystemClient} the handler uses internally
     */

    public GameSystemClientMasterServiceHandler(GameSystemClient gameSystemClient){
        Preconditions.checkNotNull(gameSystemClient, "gameSystemClient");
        mGameSystemClient = gameSystemClient;
    }


    public GetServiceVersionTResponse getServiceVersion(GetServiceVersionTOptions options) {
        return new GetServiceVersionTResponse(Constants.FILE_SYSTEM_MASTER_CLIENT_SERVICE_VERSION);
    }


    @Override
    public CheckCacheChangeTResponse checkCacheChange(final Map<String, Boolean> fileList, String user)
            throws TException {
        return RpcUtils.call(LOG, (RpcUtils.RpcCallableThrowsIOException<CheckCacheChangeTResponse>)()->{
            ArrayList<String> cachingList= mGameSystemClient.checkCacheChange(fileList, user);
            return new CheckCacheChangeTResponse(cachingList);
        });
    }

    @Override
    public LoadTResponse load(String path) throws AlluxioTException, TException {
        return RpcUtils.call(LOG, (RpcUtils.RpcCallableThrowsIOException<LoadTResponse>)()->{
            AlluxioURI uri = new AlluxioURI(path);
            OpenFileOptions options = OpenFileOptions.defaults().setReadType(ReadType.CACHE_PROMOTE);
            mGameSystemClient.openFile(uri,options);
            return new LoadTResponse();
        });
    }
}
