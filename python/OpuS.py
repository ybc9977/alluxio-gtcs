import sys
from VCG_Mech import VCG_Mech
from Isolated_Allocator import Isolated_Allocator
from FairRide_allocator import FairRide_allocator
import numpy as np
import datetime
import math


def OpuS():
    f = open("prefs.txt", 'r')
    R = float(f.readline())
    log = open("OpuS_log.txt", 'a')  # append
    now = datetime.datetime.now()
    log.write("###################Start: %s ####################\n" % now)


    #parse user preferences
    prefs = f.readlines()
    f.close()
    user_number = len(prefs)
    file_number = len(prefs[0].split(','))
    log.write("User number: %s\n" %user_number)
    log.write("File number: %s\n" % file_number)
    file_size_set = np.ones(file_number,dtype=float)

    PMatrix = np.random.rand(user_number, file_number)
    for user_index in range(0, user_number):
        user_preference_set_string = prefs[user_index].split(",")
        user_preference_set = map(float, user_preference_set_string)
        PMatrix[user_index] = np.array(user_preference_set)
        log.write("User %s preferences: %s\n" % (user_index,user_preference_set))
    log.flush()
    # if mode == "Isolated":
    #     print "Isolated"
    #     Isolated_Allocator(PMatrix, file_size_set, R)
    #     return
    # elif mode == "FairRide":
    #     print "FairRide"
    #     FairRide_allocator(PMatrix, file_size_set, R)
    #     return
    # else: # either OpuS or OpuS_Isolated
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
        log.write("Opus Succeeded")
        (PMatrix_unified, cached_ratio, access_factor, final_utility) = result
        log.write("Opus cached ratio: %s" % cached_ratio)
        log.write("Opus access factor: %s" % access_factor)
        cached_ratio_string = ','.join(str("{:10.4f}".format(e)) for e in cached_ratio)
        access_factor_string = ','.join(str("{:10.4f}".format(e)) for e in access_factor)
        f=open("ratio_opus.txt",'w')
        f.write(cached_ratio_string)
        f.close()
        f=open("factor_opus.txt",'w')
        f.write(access_factor_string)
        f.close()
        print(cached_ratio_string)
        print(access_factor_string)

    log.write("###################End########################################\n\n\n")
    log.close()


if __name__ == "__main__":
    OpuS()
    #OpuS(["1,1,2", "1,2,5", "2,3,4", "3,4,10", "4,3,20"])
    #OpuS(["9394592,9394592,9394592,9394592,9394592,9394592,9394592,9394592,9394592","0,3,0,7,3,1,0,3,0", "1,2,4,2,2,0,1,3,2", "2,2,3,3,0,0,4,1,1"])
