namespace java alluxio.thrift

include "common.thrift"
include "exception.thrift"


struct CheckCacheChangeTResponse{
        1: list<string> cachingList
}
struct LoadTResponse{
}

//Client offer service to Master
service GameSystemClientMasterService extends common.AlluxioService {

    CheckCacheChangeTResponse checkCacheChange(
        1: map<string,bool> fileList,
        2: string user
    )
    throws (1: exception.AlluxioTException e)

    LoadTResponse load(
        1: string path
    )
    throws (1: exception.AlluxioTException e)

}