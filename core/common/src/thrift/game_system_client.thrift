namespace java alluxio.thrift

include "common.thrift"
include "exception.thrift"

struct CheckCacheChangeTOptions{
        1: bool implement,
        2: i64 userId
        //Note that the userId item in the communication process is unnecessary
}

struct CheckCacheChangeTResponse{
        1: list<i64> cachingList
}

//Client offer service to Master
service GameSystemClientMasterService extends common.AlluxioService {

    CheckCacheChangeTResponse checkCacheChange(
        1: map<i64,bool> fileList,
        2: CheckCacheChangeTOptions options
    )
    throws (1: exception.AlluxioTException e)

}