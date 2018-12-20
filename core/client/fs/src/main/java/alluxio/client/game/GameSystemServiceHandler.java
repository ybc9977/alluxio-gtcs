package alluxio.client.game;

import alluxio.AlluxioURI;
import alluxio.Constants;
import alluxio.RpcUtils;
import alluxio.client.ReadType;
import alluxio.client.file.FileInStream;
import alluxio.client.file.FileSystemContext;
import alluxio.client.file.options.OpenFileOptions;
import alluxio.client.file.policy.LocalFirstPolicy;
import alluxio.collections.Pair;
import alluxio.thrift.*;
import com.google.common.base.Preconditions;
import com.google.common.io.Closer;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
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

//    @Override
//    public CheckCacheChangeTResponse checkCacheChange(final Map<String, Boolean> fileList, int QUOTA)
//            throws TException {
//        return RpcUtils.call(LOG, (RpcUtils.RpcCallableThrowsIOException<CheckCacheChangeTResponse>)()->{
//            ArrayList<String> cachingList= mGameSystemServer.checkCacheChange(fileList,QUOTA);
//            return new CheckCacheChangeTResponse(cachingList);
//        });
//    }

//    @Override
//    public GetPrefTResponse getPref(GetPrefTOptions options) throws TException {
//        return RpcUtils.call(LOG, (RpcUtils.RpcCallableThrowsIOException<GetPrefTResponse>)
//                ()-> new GetPrefTResponse(mGameSystemServer.getPref()));
//    }

    @Override
    public LoadTResponse load(String path) throws TException {
        return RpcUtils.call(LOG, (RpcUtils.RpcCallableThrowsIOException<LoadTResponse>)()->{
            AlluxioURI uri = new AlluxioURI(path);
            OpenFileOptions options = OpenFileOptions.defaults().setReadType(ReadType.CACHE_PROMOTE).setCacheLocationPolicy(new LocalFirstPolicy());
            if (mGameSystemServer.getStatus(new AlluxioURI(path)).getInAlluxioPercentage() == 100) {
                // The file has already been fully loaded into Alluxio.
                System.out.println(path+ " already in Alluxio fully");
                return new LoadTResponse();
            }
            Closer closer = Closer.create();
            try {
                FileInStream in = closer.register(mGameSystemServer.openFile(uri, options));
                byte[] buf = new byte[8 * Constants.MB];
                while (in.read(buf) != -1) {
                }
            } catch (Exception e) {
                throw closer.rethrow(e);
            } finally {
                closer.close();
            }
            return new LoadTResponse();
        });
    }

    @Override
    public AccessTResponse access(List<Double> prefs, String mode, List<Double> cacheRatio, List<Double> accessFactor) throws TException {
        return RpcUtils.call(LOG, (RpcUtils.RpcCallableThrowsIOException<AccessTResponse>)()->{
            Pair pair= mGameSystemServer.access(prefs,mode, cacheRatio, accessFactor);
            return new AccessTResponse((double)pair.getFirst(),(long)pair.getSecond());
        });
    }

//    @Override
//    public AccessFairRideTResponse accessFairRide(Map<String, Double> pref, List<Double> factor) throws AlluxioTException {
//        return RpcUtils.call(LOG, (RpcUtils.RpcCallableThrowsIOException<AccessFairRideTResponse>)()->{
//            Pair<Double, Long> p= mGameSystemServer.access(pref,factor);
//            return new AccessFairRideTResponse(p.getFirst(),p.getSecond());
//        });
//    }

    @Override
    public ResetTResponse reset(ResetTOptions options) throws TException {
        return RpcUtils.call(LOG, (RpcUtils.RpcCallableThrowsIOException<ResetTResponse>)()->{
            mGameSystemServer.reset();
            return new ResetTResponse();
        });
    }

}
