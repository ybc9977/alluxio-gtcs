# Input: 1. preference matrix: PMatrixUnified.
            #Row: user; Column: file
            #The sum of each row should be unified to be 1

#        2. Total cache size: R

from cvxpy import *
import numpy
import sys

def PE_allocator(PMatrixUnified, size_set, R): #(PMatrix, R):

    # n = PMatrixUnified.shape[0]
    # m = PMatrixUnified.shape[1]
    try:
        m = PMatrixUnified.shape[1]
    except:
        breakp = 1

    # ######### For testing the accuracy of the PE allocator
    # # Random Preference matrix
    # n = 3 # number of users
    # m = 3 # number of files
    # R = 2 # cache size
    # maxUtility = 5
    # # numpy.random.seed(1)
    # PMatrix = numpy.random.randint(0, maxUtility, size = (n, m))
    #
    #
    # # Construct Preference matrix
    # #PMatrix = numpy.array([[1, 0], [0, 1], [0, 1], [0, 1], [0, 1], [0, 1], [0, 1], [0, 1], [0, 1], [0, 1]])
    # #n = PMatrix.shape[0]
    # #m = PMatrix.shape[1]
    # #R = 1
    #
    # PMatrixUnified = numpy.random.rand(n, m)
    # for index_n in range(0, n):
    #     PMatrixUnified[index_n] =  PMatrix[index_n] / float(sum(PMatrix[index_n]))
    # # b = numpy.random.randn(n, 1)
    # ##############################################


    # Construct the problem.
    x = Variable(m) # allocation vector
    objective = Maximize(sum_entries(log(PMatrixUnified*x)))
    normalized_size = [size / R for size in size_set]
    # print normalized_size
    constraints = [0 <= x, x <= 1, sum_entries(reshape(normalized_size, 1, m)*x) <= 1] #
    prob = Problem(objective, constraints)
    try:
        prob.solve()
        ##print "Optimal value", result

        #print "Preference Matrix"
        #print PMatrix

        #print "Optimal var"
        #print x.value
        if prob.status == 'optimal':
            return x.value
        else:
            return False
    except:
        return False

