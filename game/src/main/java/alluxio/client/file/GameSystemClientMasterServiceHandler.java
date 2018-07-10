package alluxio.client.file;

import alluxio.RpcUtils;
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
        return new GetServiceVersionTResponse(2);
    }


    @Override
    public CheckCacheChangeTResponse checkCacheChange(final Map<String, Boolean> fileList)
            throws TException {
        return RpcUtils.call(LOG, (RpcUtils.RpcCallableThrowsIOException<CheckCacheChangeTResponse>)()->{
            ArrayList<String> cacheFileList= mGameSystemClient.checkCacheChange(fileList);
            return new CheckCacheChangeTResponse(cacheFileList);
        },"CheckCacheChange", "fileList=%s",fileList);
    }
}
