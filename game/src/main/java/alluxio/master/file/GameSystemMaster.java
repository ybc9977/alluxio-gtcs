package alluxio.master.file;

import alluxio.AbstractMasterClient;
import alluxio.Constants;
import alluxio.client.file.options.CheckCacheChangeOptions;
import alluxio.collections.Pair;
import alluxio.exception.status.AlluxioStatusException;
import alluxio.master.GameMasterListMaintainer;
import alluxio.master.MasterClientConfig;
import alluxio.thrift.AlluxioService;
import alluxio.thrift.GameSystemClientMasterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *  created by byangak on 28/06/2018
 */

public final class GameSystemMaster extends AbstractMasterClient {

    private static final Logger LOG = LoggerFactory.getLogger(GameSystemMaster.class);

    private GameSystemClientMasterService.Client mClient = null;

    public GameSystemMaster(MasterClientConfig conf) {
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

    public synchronized List<Long> checkCacheChange(Map<Long,Boolean> fileList,
    CheckCacheChangeOptions options) throws AlluxioStatusException {
        return retryRPC(() -> {
            mClient.checkCacheChange(fileList, options.toThrift());
            return null;
        }, "CheckCacheChange");
    }

    public synchronized void gameTheoreticalCommunication(Long fileId) throws AlluxioStatusException {
        ArrayList<Pair<Long,Boolean>> userList = GameMasterListMaintainer.getuser();
        while(true){
            int index = (int) (Math.random() * userList.size());
            Pair<Long, Boolean> user = userList.get(index);
            Map<Long,Boolean> file_list = GameMasterListMaintainer.getfile();
            List<Long> cache_list = null;
            cache_list = checkCacheChange(file_list, CheckCacheChangeOptions.defaults(user.getFirst()));
            if (cache_list!=null){
                for (Long file: cache_list){
                    if(file_list.containsKey(file)){
                        file_list.replace(fileId,true);
                    }
                }
                userList.set(index,user.setSecond(true));
            }else{
                userList.set(index,user.setSecond(false));
            }
            GameMasterListMaintainer.changefile(file_list);
            for (Pair<Long,Boolean> p:userList) {
                if (p.getSecond()) {
                    break;
                } else if (userList.indexOf(p) == userList.size() - 1) {
                    return;
                }
            }
        }
//        int mark = 0;
//        boolean prev = false, cur;
//        for(int indexNow = 0; indexNow<userList.size(); indexNow= (indexNow==userList.size()-1)?0:indexNow+1){
//            Long user = userList.get(indexNow);
//            Map<Long,Boolean> file_list = GameMasterListMaintainer.getfile();
//            List<Long> cache_list = null;
//            cache_list = checkCacheChange(file_list, CheckCacheChangeOptions.defaults(user));
//            if (cache_list!=null){
//                for (Long file: cache_list){
//                    if(file_list.containsKey(file)){
//                        file_list.replace(fileId,true);
//                    }
//                }
//                cur = true;
//            }else{
//                cur = false;
//            }
//            if (cur){
//                mark = -1;
//            }else if(!prev){
//                mark = indexNow;
//            }
//            GameMasterListMaintainer.changefile(file_list);
//            prev = cur;
//            if (mark>1){
//                if (indexNow == mark-1) break;
//            }else{
//                if (indexNow == mark-1+userList.size()) break;
//            }
//        }
    }
}
