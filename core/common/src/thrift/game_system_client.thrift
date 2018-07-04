namespace java alluxio.thrift

include "common.thrift"
include "exception.thrift"

struct CheckCacheChangeTOptions{
        1: bool implement,
        2: i64 userId
}

struct CheckCacheChangeTResponse{
        1: list<i64> cachingList
}


service GameSystemClientMasterService extends common.AlluxioService {

    CheckCacheChangeTResponse checkCacheChange(
        1: map<i64,bool> fileList,
        2: CheckCacheChangeTOptions options
    )
    throws (1: exception.AlluxioTException e)

}