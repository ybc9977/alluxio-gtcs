package alluxio.client.file;

import alluxio.RpcUtils;
import alluxio.thrift.*;
import com.google.common.base.Preconditions;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *  created by byangak on 28/06/2018
 */

public class GameSystemClientHandler implements
        GameSystemClientMasterService.Iface {
    private static final Logger LOG =
            LoggerFactory.getLogger(GameSystemClientHandler.class);
    //
    private final GameFileSystem mGameFileSystem;

    /**
     * Creates a new instance of {@link GameSystemClientHandler}.
     *
     * @param gameFileSystem the {@link GameFileSystem} the handler uses internally
     */

    public GameSystemClientHandler(GameFileSystem gameFileSystem){
        Preconditions.checkNotNull(gameFileSystem, "gameFileSystem");
        mGameFileSystem=gameFileSystem;
    }


    public GetServiceVersionTResponse getServiceVersion(GetServiceVersionTOptions options) {
        return new GetServiceVersionTResponse(2);
    }


    @Override
    public CheckCacheChangeTResponse checkCacheChange(Map<Long, Boolean> fileList, CheckCacheChangeTOptions options)
            throws AlluxioTException, TException {
        return RpcUtils.call(LOG, (RpcUtils.RpcCallableThrowsIOException<CheckCacheChangeTResponse>)()->{
            ArrayList<Long> cacheFileList= mGameFileSystem.checkCacheChange(fileList, options);
            return new CheckCacheChangeTResponse(cacheFileList);
        },"CheckCacheChange", "options=%s", options);
    }
}
