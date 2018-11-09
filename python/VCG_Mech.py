# VCG Mechanism
#
# input: PMatrix: preference matrix
#       size_set: file sizes
#       R: cache size



#
# 1. Find the PF allocation
# 2. Calculate the tax (or allocated Ratio)

# 3. Check the tax bound: Compare the ratio with the theoretical bound
# 4. Check sharing incentive: Calculate the utility with static portion and compare it with the utility after paying the tax



from PE_allocator import PE_allocator
import numpy

def VCG_Mech(PMatrix, size_set, R):
    n = PMatrix.shape[0]
    m = PMatrix.shape[1]

    PMatrixUnified = numpy.random.rand(n, m)
    for index_n in range(0, n):
        PMatrixUnified[index_n] = PMatrix[index_n] / (PMatrix[index_n].sum(dtype='float'))
    PE_allocation = numpy.asarray(PE_allocator(PMatrixUnified, size_set, R)).reshape(-1)
    if PE_allocation.any() == False:
        return False
    utilities = numpy.asarray(PMatrixUnified.dot(PE_allocation))

    # calculate the tax
    tax = numpy.zeros(n)
    for i in range(0, n):
        # for the case without user i
        utilities_without_i = numpy.delete(utilities, i)
        PMatrixUnified_without_i = numpy.delete(PMatrixUnified, (i), axis = 0)
        PE_allocation_prime = numpy.asarray(PE_allocator(PMatrixUnified_without_i, size_set, R)).reshape(-1)
        if PE_allocation_prime.any() == False:
            return False
        utilities_prime = numpy.asarray(PMatrixUnified_without_i .dot(PE_allocation_prime))
        tax[i] = sum(numpy.log(utilities_prime)) - sum(numpy.log(utilities_without_i))
    if tax.any() < 0: # impossible
        return False

    allocated_ratio = numpy.exp(-tax)# allocated ratio should be bounded by numpy.power(n/(n-1), 1-n)
    lost_ratio = 1 - allocated_ratio
    final_utility = allocated_ratio * utilities #checked

    ## To do: what is the static utility now? A knapsack problem!
    # sorted = -numpy.partition(-PMatrixUnified, int(numpy.ceil(R/n)))[:, :int(numpy.ceil(R/n))]
    # static_utility = numpy.sum(sorted[:, :int(R/n)], axis=1) + sorted[:, -1] * (R/n - int(numpy.floor(R/n))) # checked

    ## for debugging
    #print PMatrixUnified
    #print PE_allocation
    #print utilities
    #print "final_utility:", final_utility
    #print "static_utility", static_utility
    # print "benefit:", final_utility-static_utility
    # print "allocated ratio bound: ", numpy.power(float(n)/(n-1), 1-n)
    #print "allocated ratio:", allocated_ratio
    #print "tax bound:", (n-1)*numpy.log(float(n)/(n-1))
    #print tax

    return (PMatrixUnified, PE_allocation, allocated_ratio,final_utility)

if __name__ == "__main__":
    # # construct PMatrix for testing
    # For debugging of OpuS and FairRide
    # PMatrix = numpy.array([[ 0.03481625,0.03675048,0.40038685,0.05029014,0.03675048,0.11218569,0.06576402,0.1450677,0.04642166,0.07156673],
   #                 [ 0.06150794,0.0515873,0.05357143,0.18055556,0.03571429,0.03174603,0.06944444,0.11507937,0.35515872,0.04563492],
   #                 [ 0.0341556,0.05502846,0.0341556,0.12523719,0.04174573,0.17267552,0.03795066,0.35294118,0.07590133,0.07020873],
   #                 [ 0.02994012,0.1756487,0.10578842,0.05588822,0.05788423,0.0259481,0.38522954,0.05588822,0.08782435,0.01996008]])
    #R = 5.0 # must be float!
    #size_set = [1,1,1,1,1,1,1,1,1,1]

    ## For FairRide cheating
    # PMatrix = numpy.array([[0.57,0,0,0,0.43], [0,0.43,0,0,0.57],[0,0,0.43,0,0.57],[0,0,0,0.43,0.57], [1,0,0,0,0],[0,1,0,0,0],[0,0,1,0,0],[0,0,0,1,0],])
    # For LRU cheating
    # PMatrix = numpy.array([[0.2,0.2,0.3,0.3,0,0], [0,0,0.3,0.3,0.2,0.2]])
    #PMatrix = numpy.array([[0.33,0.33,0.17,0.17,0,0], [0,0,0.3,0.3,0.2,0.2]])
    #size_set = [1,1,1,1,1,1]
    #R = 3.0

    # For FairRide cheating
    #PMatrix = numpy.array([[0.43,0,0,0,0.57], [0,0.43,0,0,0.57],[0,0,0.43,0,0.57], [0,0,0,0.43,0.57],[1,0,0,0,0],[0,1,0,0,0],[0,0,1,0,0],[0,0,0,1,0]])
    ProbMatrix = numpy.array([[1, 0, 0], [0.45, 0.55, 0], [0, 0.55, 0.45], [0, 0.55, 0.45]])
    ProbMatrix_fake = numpy.array([[1, 0, 0], [0.55, 0.45, 0], [0, 0.55, 0.45], [0, 0.55, 0.45]])
    size_set = [1,1,1]
    R = 2.0
    VCG_Mech(ProbMatrix_fake,size_set, R)  # sys.argv[:2]
