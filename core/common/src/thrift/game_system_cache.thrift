namespace java alluxio.thrift

include "common.thrift"
include "exception.thrift"

struct FileSystemMasterCommonTOptions {
  1: optional i64 syncIntervalMs
  2: optional i64 ttl
  3: optional common.TTtlAction ttlAction
}

struct FreeTOptions {
  1: optional bool recursive
  2: optional bool forced
  3: optional FileSystemMasterCommonTOptions commonOptions
}
struct FreeTResponse {}

service GameSystemCacheService extends common.AlluxioService{

FreeTResponse free(
    /** the path of the file or directory */ 1: string path,
    // This is deprecated since 1.5 and will be removed in 2.0. Use FreeTOptions.
    /** whether to free recursively */ 2: bool recursive,
    /** the options for freeing a path */ 3: FreeTOptions options,
    )
    throws (1: exception.AlluxioTException e)

}