import os
import sys
from Isolated_Allocator import Isolated_Allocator
from FairRide_allocator import FairRide_allocator
import numpy as np
import datetime
import math


def FairRide():
    cwd = os.path.abspath(os.path.dirname(os.path.abspath(__file__)) + os.path.sep + ".")
    f = open(cwd+"prefs.txt", 'r')
    R = float(f.readline())
    log = open(cwd+"FairRide_log.txt", 'a')  # append
    now = datetime.datetime.now()
    log.write("###################Start: %s ####################\n" % now)


    #parse user preferences
    prefs = f.readlines()
    f.close()
    user_number = len(prefs)
    file_number = len(prefs[0].split(','))
    log.write("User number: %s\n" %user_number)
    log.write("File number: %s\n" % file_number)
    file_size_set = np.ones([file_number, 1])

    PMatrix = np.random.rand(user_number, file_number)
    for user_index in range(0, user_number):
        user_preference_set_string = prefs[user_index].split(",")
        user_preference_set = map(float, user_preference_set_string)
        PMatrix[user_index] = np.array(user_preference_set)
        log.write("User %s preferences: %s\n" % (user_index,user_preference_set))
    log.flush()


    print "FairRide"
    (cachedFractions, AccessFactorMatrix) = FairRide_allocator(PMatrix, file_size_set, R)
    log.write("FairRide Succeeded")

    log.write("FairRide cached ratio: %s" % cachedFractions)
    log.write("FairRide access factor: %s" % AccessFactorMatrix)

    cachedFractionString = ','.join(str("{:10.4f}".format(e)) for e in cachedFractions)
    f=open(cwd+"ratio_fairride.txt",'w')
    f.write(cachedFractionString)
    f.close()

    f=open(cwd+"factor_fairride.txt",'w')
    for user_index in range(0,user_number):
        AccessFactorString = ','.join(str("{:10.4f}".format(e)) for e in AccessFactorMatrix[user_index, :])
        f.write("%s\n" % AccessFactorString)
    f.close()


    log.write("###################End########################################\n\n\n")
    log.close()
    return

if __name__ == "__main__":
    FairRide()
