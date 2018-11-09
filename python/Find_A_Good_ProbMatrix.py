import sys
from VCG_Mech import VCG_Mech
from Isolated_Allocator import Isolated_Allocator
from FairRide_allocator import FairRide_allocator
import numpy
import pickle

def Find_A_Good_ProbMatrix():
    # generate preference order
    n = 20  # number of users
    m = 60  # number of files
    ProbMatrix = numpy.zeros([n, m])
    R = 50
    file_size_set = numpy.ones(m)

    for repeat in range(0,5000):
        for i in range(0, n):
            numbers = list(numpy.random.zipf(1.1, 5000))
            prob = numpy.zeros(m)
            total_valid = 0
            for j in range(0, m):
                prob[j] = numbers.count(j+1)
                total_valid += prob[j]
            prob = [this_prob / total_valid for this_prob in prob]
            print sum(prob)
            ProbMatrix[i, :] = numpy.random.permutation(prob)
        with open('pm1.pickle') as f:  # Python 3: open(..., 'rb')
            ProbMatrix = pickle.load(f)
        #print ProbMatrix
        fairRideUtility = sum(FairRide_allocator(ProbMatrix,file_size_set,R))
        print fairRideUtility
        (PMatrix_unified, cached_ratio, access_factor, final_utility) = VCG_Mech(ProbMatrix, file_size_set, R)
        opuSUtility = sum(final_utility)
        print opuSUtility
        print (opuSUtility-fairRideUtility)/(fairRideUtility)
        print access_factor

if __name__ == "__main__":
    Find_A_Good_ProbMatrix()
