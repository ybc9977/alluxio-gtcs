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
        1: double ratio
        2: i64 time
}

struct AccessFairRideTResponse{
        1: double ratio
        2: i64 time
}

struct ResetTOptions{
}
struct ResetTResponse{
}

//Client offer service to Master
service GameSystemCacheService extends common.AlluxioService {

    CheckCacheChangeTResponse checkCacheChange(
        1: map<string,bool> fileList
        2: i32 QUOTA
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
        2: string mode
    )
    throws (1: exception.AlluxioTException e)

    AccessFairRideTResponse accessFairRide(
        1: map<string,double> prefList
        2: list<double> factor
    )

    ResetTResponse reset(
        1: ResetTOptions options
    )
    throws (1: exception.AlluxioTException e)

}