package alluxio.master.game;

import alluxio.AbstractClient;
import alluxio.AlluxioURI;
import alluxio.Constants;
import alluxio.collections.Pair;
import alluxio.exception.AccessControlException;
import alluxio.exception.FileDoesNotExistException;
import alluxio.exception.InvalidPathException;
import alluxio.exception.UnexpectedAlluxioException;
import alluxio.exception.status.AlluxioStatusException;
import alluxio.master.file.FileSystemMaster;
import alluxio.master.file.options.FreeOptions;
import alluxio.thrift.*;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.distribution.ZipfDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.IntStream;

/**
 *  created by byangak on 30/07/2018
 *
 *  Game System Master side client class, one instance to communicate with certain Game System Server
 */

public class GameSystemClient extends AbstractClient {
    private static final Logger LOG = LoggerFactory.getLogger(GameSystemMaster.class);

    private GameSystemCacheService.Client mClient = null;

    private String mUserId;

    private List<Double> mPref = new ArrayList<>();
    private int[] mSortedIndices;

    private List<Integer> mCachedFileIds = new ArrayList<>(); // the files that are cached by this client (in the game)

    private boolean delegate = true;

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



    private List<Double> getPref() throws AlluxioStatusException {
//        if (delegate)
//            return mPref;
//        else
//            mPref = retryRPC(() -> mClient.getPref(new GetPrefTOptions()).getPref(), "GetPref");
//            return mPref;
        return mPref;
    }

//    public void reset() throws AlluxioStatusException {
//        if (delegate)
//            shuffle = true;
//        else
//            retryRPC(() -> mClient.reset(new ResetTOptions()), "Reset");
//    }

    protected List<Double> updatePref(int fileNumber){
        mPref.clear();
//        mCachedFileIds.clear();
        ZipfDistribution zd = new ZipfDistribution(fileNumber,1.05);
        for(int i=1;i<=fileNumber;i++) {
            mPref.add(zd.probability(i));
        }
        Collections.shuffle(mPref);
        mSortedIndices = IntStream.range(0, mPref.size())
                .boxed().sorted((i, j) -> mPref.get(j).compareTo(mPref.get(i)) ) // descending order
                .mapToInt(ele -> ele).toArray();
        return mPref;
    }

    // return whether the caching decisions have changed
    public boolean poll(boolean[] cacheFlag, int quota){
        //reset the status of files cached by this user
        for(int fileId: mCachedFileIds)
            cacheFlag[fileId] = false;

        // get the current cache decisions.
        List<Integer> cachedFileIds = new ArrayList<>();

        for(int index:mSortedIndices) { // in descending order of preference
//            System.out.println(mUserId+" Quota: "+quota);
            if(quota>0) {
                if (!cacheFlag[index]) {
                    cacheFlag[index] = true;
                    quota--;
                    cachedFileIds.add(index);
                }
            } else
                break;

        }
        boolean change = cachedFileIds.equals(mCachedFileIds);
        mCachedFileIds = new ArrayList<>(cachedFileIds);
        return change;
    }

    synchronized Pair<Double, Long> access(String mode, List<Double> cachedRatio, List<Double> accessFactor) throws AlluxioStatusException {
        Object rpc = retryRPC(() -> mClient.access(mPref,mode,cachedRatio,accessFactor),"Access");
        return new Pair<>(((AccessTResponse) rpc).getRatio(),((AccessTResponse) rpc).getTime());
    }

}
