namespace java alluxio.thrift

include "common.thrift"
include "exception.thrift"


struct CheckCacheChangeTResponse{
        1: list<string> cachingList
}
struct GetPrefTResponse{
        1: map<string, double> pref
}
struct GetPrefTOptions{

}
struct LoadTResponse{
}
struct AccessTResponse{
        1: map<string,i32> access
}


//Client offer service to Master
service GameSystemCacheService extends common.AlluxioService {

    CheckCacheChangeTResponse checkCacheChange(
        1: map<string,bool> fileList
    )
    throws (1: exception.AlluxioTException e)

    GetPrefTResponse getPref(
        1: GetPrefTOptions options
    )
    throws (1: exception.AlluxioTException e)

    LoadTResponse load(
        1: string path
    )
    throws (1: exception.AlluxioTException e)

    AccessTResponse access(
        1: map<string,double> prefList
    )
    throws (1: exception.AlluxioTException e)
}