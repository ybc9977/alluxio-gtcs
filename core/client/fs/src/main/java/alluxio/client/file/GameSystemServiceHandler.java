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

public class GameSystemServiceHandler implements
        GameSystemCacheService.Iface {
    private static final Logger LOG =
            LoggerFactory.getLogger(GameSystemServiceHandler.class);
    //
    private final GameSystemServer mGameSystemServer;

    /**
     * Creates a new instance of {@link GameSystemServiceHandler}.
     *
     * @param gameSystemServer the {@link GameSystemServer} the handler uses internally
     */

    public GameSystemServiceHandler(GameSystemServer gameSystemServer){
        Preconditions.checkNotNull(gameSystemServer, "gameSystemServer");
        mGameSystemServer = gameSystemServer;
    }


    public GetServiceVersionTResponse getServiceVersion(GetServiceVersionTOptions options) {
        return new GetServiceVersionTResponse(Constants.GAME_SYSTEM_SERVICE_VERSION);
    }

    @Override
    public CheckCacheChangeTResponse checkCacheChange(final Map<String, Boolean> fileList)
            throws TException {
        return RpcUtils.call(LOG, (RpcUtils.RpcCallableThrowsIOException<CheckCacheChangeTResponse>)()->{
            ArrayList<String> cachingList= mGameSystemServer.checkCacheChange(fileList);
            return new CheckCacheChangeTResponse(cachingList);
        });
    }

    @Override
    public GetPrefTResponse getPref(GetPrefTOptions options) throws AlluxioTException, TException {
        return RpcUtils.call(LOG, (RpcUtils.RpcCallableThrowsIOException<GetPrefTResponse>)
                ()-> new GetPrefTResponse(mGameSystemServer.getPref()));
    }

    @Override
    public LoadTResponse load(String path) throws AlluxioTException, TException {
        return RpcUtils.call(LOG, (RpcUtils.RpcCallableThrowsIOException<LoadTResponse>)()->{
            AlluxioURI uri = new AlluxioURI(path);
            OpenFileOptions options = OpenFileOptions.defaults().setReadType(ReadType.CACHE_PROMOTE);
            mGameSystemServer.openFile(uri,options);
            return new LoadTResponse();
        });
    }

    @Override
    public AccessTResponse access(Map<String, Double> prefList) throws AlluxioTException, TException {
        return RpcUtils.call(LOG, (RpcUtils.RpcCallableThrowsIOException<AccessTResponse>)()->{
            Map<String, Integer> list= mGameSystemServer.accessFile(prefList);
            return new AccessTResponse(list);
        });
    }

}
