namespace java alluxio.thrift

include "common.thrift"
include "exception.thrift"


struct CheckCacheChangeTResponse{
        1: list<string> cachingList
}

//Client offer service to Master
service GameSystemClientMasterService extends common.AlluxioService {

    CheckCacheChangeTResponse checkCacheChange(
        1: map<string,bool> fileList
    )
    throws (1: exception.AlluxioTException e)

}