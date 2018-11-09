import sys
from VCG_Mech import VCG_Mech
from Isolated_Allocator import Isolated_Allocator
from FairRide_allocator import FairRide_allocator
import numpy as np
import datetime
import math

# Parameters:
## 0: String, mode, OpuS, FairRide, or Isolated
## 1:String, file sizes, separated by comma;
## 2+:String, each line represents the cache preferences of a user, separated by comma;

def OpuS(argv):

    #R = 0.03 * math.pow(10,9)  # 30M cache for LRU strategy
    #R = 0.02 * 0.9394592 * math.pow(10,9)  # 20M cache for FairRide strategy
    #R = 0.5 * 0.9394592 * math.pow(10,9) # 500M for efficiency tests
    R = 15.0 # for stress test with 30 files
    # print R

    log = open("OpuS_log.txt", 'a')  # append
    now = datetime.datetime.now()
    log.write("###################Start: %s ####################\n" % now)

    # log.write("")
    mode = argv[0]
    log.write("mode: %s\n" % mode)

    # parse file size
    file_size_string = argv[1]
    file_size_set_string = file_size_string.split(",")
    file_size_set = map(long, file_size_set_string)
    file_number = len(file_size_set)
    log.write("Received file size vector: %s\n" % file_size_set)
    #parse user preferences
    user_number = len(argv) - 2
    PMatrix = np.random.rand(user_number, file_number)
    for user_index in range(0, user_number):
        user_preference_set_string = argv[user_index+2].split(",")
        user_preference_set = map(float, user_preference_set_string)
        PMatrix[user_index] = np.array(user_preference_set)
        log.write("Received access history of user : %s\n" % file_size_set)

    log.flush()
    if mode == "Isolated":
        print "Isolated"
        Isolated_Allocator(PMatrix, file_size_set, R)
        return
    elif mode == "FairRide":
        print "FairRide"
        FairRide_allocator(PMatrix, file_size_set, R)
        return
    else: # either OpuS or OpuS_Isolated
        opus_success_flag = True
        result = False
        try:
            result = VCG_Mech(PMatrix, file_size_set, R)
        except:
            opus_success_flag = False
        if result == False:
             opus_success_flag = False
        if opus_success_flag == False:
            print "OpuS_Isolated"
            log.write("Opus Failed")
            Isolated_Allocator(PMatrix, file_size_set, R)
            return
        else:
            print "OpuS"
            log.write("Opus Suceceeded")
            (PMatrix_unified, cached_ratio, access_factor, final_utility) = result
            log.write("Opus cached ratio: %s" % cached_ratio)
            log.write("Opus access factor: %s" % access_factor)
            cached_ratio_string = ','.join(str("{:10.4f}".format(e)) for e in cached_ratio)
            access_factor_string = ','.join(str("{:10.4f}".format(e)) for e in access_factor)
            print(cached_ratio_string)
            print(access_factor_string)
    # log.write("effective cache hit ratio %s ")  # sum (size * cached_ratio * unified_preference * access_factor)
    # size elmentwise cached_ratio
    # if failed, what to do if failed.

    # print PMatrix
    # log.write(PMatrix)

    log.write("###################End########################################\n\n\n")
    log.close()


if __name__ == "__main__":
    OpuS(sys.argv[1:])
    #OpuS(["1,1,2", "1,2,5", "2,3,4", "3,4,10", "4,3,20"])
    #OpuS(["9394592,9394592,9394592,9394592,9394592,9394592,9394592,9394592,9394592","0,3,0,7,3,1,0,3,0", "1,2,4,2,2,0,1,3,2", "2,2,3,3,0,0,4,1,1"])
