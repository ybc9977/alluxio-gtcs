# Input: 1. preference matrix: PMatrix.
# Row: user; Column: file
# The sum of each row should be unified to be 1

#        2. Total cache size: R

import numpy as np
import math
import datetime
import pickle

def FairRide_allocator(PMatrix, file_size_set, R):
    n = PMatrix.shape[0]
    m = PMatrix.shape[1]
    PMatrixUnified = np.zeros([n,m])
    PreferenceOrder = np.zeros([n,m])
    for index_n in range(0, n):
        PMatrixUnified[index_n] = PMatrix[index_n] / (PMatrix[index_n].sum(dtype='float'))
        PreferenceOrder[index_n] = sorted(range(m),key=lambda x:-PMatrix[index_n,x]) # descend order
    PreferenceOrder = PreferenceOrder.astype(int)
    PaymentMatrix = np.zeros([n,m])# normarlized
    AccessFactorMatrix = np.zeros([n,m]) # normalized
    next_file_to_cache = np.zeros(n).astype(int) # the next wanted file for each user, iterates through the PreferenceOrder matrix
    user_quota = np.ones(n) * float(R) / n
    unsaturated_users = list(range(0,n))
    common_quota = 0  # saved quota from those saturated users, for redistribution

    # iteratively bid and pay
    # until every user is either saturated or with no quota left
    while True:
        unsaturated_users, common_quota, user_quota, PaymentMatrix = Bidding(n, m, next_file_to_cache, unsaturated_users, PaymentMatrix, user_quota, PreferenceOrder, PMatrixUnified, file_size_set)
        next_file_to_cache, user_quota, PaymentMatrix = Payments(n, m, PaymentMatrix, PreferenceOrder, PMatrixUnified, next_file_to_cache, file_size_set)
        # now redistribute the common_quota among the unsaturated users
        if len(unsaturated_users) > 0:
            user_quota[unsaturated_users] += common_quota / float(len(unsaturated_users))
        common_quota = 0
        if len(unsaturated_users) == 0 or sum(user_quota) <= 1e-8 * R:
            break



    # utilities_without_blocking, utilities_fairride = Calculate_Utility(n, m, PaymentMatrix, PMatrixUnified)
    cachedFractions = get_CachedFractions(n, m, PaymentMatrix)
    cachedFractionString = ','.join(str("{:10.4f}".format(e)) for e in cachedFractions)
    print cachedFractionString
    AccessFactorMatrix = Calculate_Access_Factor_Matrix(n, m, PaymentMatrix)
    for user_index in range(0,n):
        AccessFactorString = ','.join(str("{:10.4f}".format(e)) for e in AccessFactorMatrix[user_index, :])
        print AccessFactorString

    # print PMatrixUnified
    # print PaymentMatrix
    # print AccessFactorMatrix
    #print utilities_fairride
    (utilities_without_blocking, utilities_fairride) = Calculate_Utility(n,m,PaymentMatrix,PMatrixUnified)
    return utilities_fairride

# process of bidding. Over-bidding might happen in this process.
# saturated_users vector will be updated.
# After the process, every user is either saturated or with no quota left
def Bidding(n, m, next_file_to_cache, unsaturated_users, PaymentMatrix, user_quota, PreferenceOrder, PMatrixUnified, file_size_set):
    common_quota = 0
    for user_index in range(0, n):
        quota = user_quota[user_index]
        if user_index not in unsaturated_users: # this user is saturated
            common_quota += quota
            continue
        next_file_order = next_file_to_cache[user_index]
        next_file_index = PreferenceOrder[user_index, next_file_order]
        next_file_size = file_size_set[next_file_index]
        if PMatrixUnified[user_index, next_file_index] == 0: # saturaed
            common_quota += quota
            unsaturated_users.remove(user_index)
            continue

        next_file_fraction = PaymentMatrix[user_index, next_file_index]
        if abs(next_file_fraction - 1) > 1e-8: # this file has not been fully cached for this user, fill it.
            add_on = min(quota, (1 - next_file_fraction)*next_file_size)
            PaymentMatrix[user_index, next_file_index] += (float)(add_on) / next_file_size
            quota -= add_on
        else: # this should never happen.  for debugging
            bug = 1
        while quota > 0: # use up the remaining quota
            next_file_order += 1
            next_file_index = PreferenceOrder[user_index, next_file_order]
            next_file_size = file_size_set[next_file_index]
            if PMatrixUnified[user_index, next_file_index] != 0 and next_file_order <= m:
                bid = min(quota ,next_file_size)
                PaymentMatrix[user_index, next_file_index] = (float)(bid) / next_file_size
                quota -= bid
            else: # saturated. the left quota should be added to the common_quota
                common_quota += quota
                unsaturated_users.remove(user_index)
                break
    user_quota = np.zeros(n)
    return unsaturated_users, common_quota, user_quota, PaymentMatrix


# Given users' bids, calculate the payment: to remove over-bidding
# next_file_to_cache will be updated
# user_quota should be zero before the process, and recalculated in the Payment process
# PaymentMatrix will be updated

def Payments(n, m, PaymentMatrix, PreferenceOrder, PMatrixUnified, next_file_to_cache, file_size_set):
    user_quota = np.zeros(n)
    for file_index in range(0,m):
        Payment_Vector = PaymentMatrix[:, file_index]
        original_Payment_Vector = np.copy(Payment_Vector)
        cached_fraction = sum(Payment_Vector)
        if cached_fraction > 1 :  # over bid
            New_Payment_Vector = np.zeros(len(Payment_Vector))
            accumulatedFraction = 0  # starting from the smallest bid, till the file is fully cached
            while accumulatedFraction < 1:
                non_zeros_payments = Payment_Vector[np.where(Payment_Vector > 0)]
                smallest_bid = np.min(non_zeros_payments) # smallest_bid larger than zero
                affordable_users, = np.where(Payment_Vector >= smallest_bid)
                this_round_payment = np.min([smallest_bid, (1 - accumulatedFraction) / float(len(affordable_users))])  # no more than 1
                count_of_affordable_users = len(affordable_users)
                New_Payment_Vector[affordable_users] = New_Payment_Vector[affordable_users] + this_round_payment
                Payment_Vector[affordable_users] = Payment_Vector[affordable_users] - this_round_payment  # remove the smallest bid value
                accumulatedFraction += this_round_payment * count_of_affordable_users
            PaymentMatrix[:, file_index] = New_Payment_Vector
            # the quota of the over-bid users should be added
            user_quota += (original_Payment_Vector - New_Payment_Vector) * file_size_set[file_index] # check
        else:
            1  # do nothing, just return the same payment vector
            PaymentMatrix[:, file_index] = Payment_Vector

    # given the PaymentMatrix, update the next_file_to_cache vector
    for user_index in range(0,n):
        # if 1. the file is fully cached, 2. the payment of this user is (one of) the largest among all users, then he fully pays.
        this_file_order = next_file_to_cache[user_index]
        while True:
            if this_file_order > m:
                next_file_to_cache[user_index] = m
                break
            this_file_index = PreferenceOrder[user_index, this_file_order]
            if abs(PMatrixUnified[user_index, this_file_index]) < 1e-8: # this user is now saturated
                next_file_to_cache[user_index] = this_file_order
                break
            Payment_Vector = PaymentMatrix[:, this_file_index]
            if abs(sum(Payment_Vector) - 1) <= 1e-8 and PaymentMatrix[user_index, this_file_index] == max(Payment_Vector):
                this_file_order += 1
                continue
            else:
                next_file_to_cache[user_index] = this_file_order
                break
    return next_file_to_cache, user_quota, PaymentMatrix


# calculate the utilities of each user. With blocking probability of 1/(N+1)
def Calculate_Utility(n, m, PaymentMatrix, PMatrixUnified):
    utilities_without_blocking = np.zeros(n)
    utilities_fairride = np.zeros(n)
    for file_index in range(0, m):
        Payment_Vector = PaymentMatrix[:,file_index]
        for user_index in range(0, n):
            pay = Payment_Vector[user_index]
            lower_pay = Payment_Vector[np.where(Payment_Vector <= pay)]
            higher_pay = Payment_Vector[np.where(Payment_Vector > pay)]
            complete_access = sum(lower_pay) + pay*len(higher_pay)
            blocked_access = sum(higher_pay) * len(higher_pay) / float(1+ len(higher_pay))  # not accurate yet
            utilities_without_blocking[user_index] += sum(Payment_Vector) * PMatrixUnified[user_index, file_index]
            utilities_fairride[user_index] += (complete_access + blocked_access) * PMatrixUnified[user_index, file_index]
    return utilities_without_blocking, utilities_fairride

def Calculate_Access_Factor_Matrix(n, m, PaymentMatrix):
    AccessFactorMatrix = np.zeros([n, m])
    for file_index in range(0, m):
        Payment_Vector = PaymentMatrix[:,file_index]
        for user_index in range(0, n):
            pay = Payment_Vector[user_index]
            lower_pay = Payment_Vector[np.where(Payment_Vector <= pay)]
            higher_pay = Payment_Vector[np.where(Payment_Vector > pay)]
            complete_access = sum(lower_pay)+ pay*len(higher_pay)
            blocked_access = sum(higher_pay) * len(higher_pay) / float(1+ len(higher_pay))  # not accurate yet
            AccessFactorMatrix[user_index, file_index] += (complete_access + blocked_access)
    return AccessFactorMatrix


def get_CachedFractions(n, m, PaymentMatrix):
    cachedFractions = np.zeros(m)
    for file_index in range(0, m):
        cachedFractions[file_index]  = sum(PaymentMatrix[:,file_index])
    return cachedFractions



if __name__ == "__main__":
    # # construct PMatrix for testing
    #PMatrix = np.array([[0.43,0,0,0,0.57], [0,0.43,0,0,0.57],[0,0,0.43,0,0.57], [0,0,0,0.43,0.57],[1,0,0,0,0],[0,1,0,0,0],[0,0,1,0,0],[0,0,0,1,0]])
    #PMatrix = np.array([[0.8,0,0,0,0.2], [0,0.43,0,0,0.57],[0,0,0.43,0,0.57], [0,0,0,0.43,0.57],[1,0,0,0,0],[0,1,0,0,0],[0,0,1,0,0],[0,0,0,1,0]])
    #PMatrix = np.array([[0,0,1], [1,0,0], [0,0,1], [0,0,1], [0,1,0]])
    #size_set = [9394592,9394592,9394592]
    with open('pm1.pickle') as f:  # Python 3: open(..., 'rb')
            ProbMatrix = pickle.load(f)
    size_set = np.ones(60)
    R = 50.0 # 500M for efficiency tests
    utilities_fairride = FairRide_allocator(ProbMatrix,size_set, R);  # sys.argv[:2]
    print utilities_fairride

