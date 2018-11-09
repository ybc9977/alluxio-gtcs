
import numpy as np

# use LFU
def Isolated_Allocator(PMatrix, file_size_set, R):
    n = PMatrix.shape[0]
    m = PMatrix.shape[1]
    PMatrixUnified = np.zeros([n,m])
    PreferenceOrder = np.zeros([n,m])
    CachedLists = []
    for index_n in range(0, n):
        PMatrixUnified[index_n] = PMatrix[index_n] / (PMatrix[index_n].sum(dtype='float'))
        PreferenceOrder[index_n] = sorted(range(m),key=lambda x:-PMatrix[index_n,x]) # descend order
        order = PreferenceOrder[index_n].astype(int)
        remaining_quota = float(R) / n
        list = []
        for this_order in range(0, m):
            this_index = order[this_order]
            if remaining_quota >= file_size_set[this_index] and abs(PMatrixUnified[index_n,this_index])>1e-8:
                remaining_quota -= file_size_set[this_index]
                list.append(this_index)
            # else:
              #  break
        CachedLists.append(list)
    for index_n in range(0, n):
        cachedlist_string = ','.join(str("{:d}".format(e)) for e in CachedLists[index_n])
        print cachedlist_string


if __name__ == "__main__":
    # # construct PMatrix for testing
    PMatrix = np.array([[0.43,0,0,0,0.57], [0,0.43,0,0,0.57],[0,0,0.43,0,0.57], [0,0,0,0.43,0.57],[1,0,0,0,0],[0,1,0,0,0],[0,0,1,0,0],[0,0,0,1,0]])
    #PMatrix = np.array([[0.8,0,0,0,0.2], [0,0.43,0,0,0.57],[0,0,0.43,0,0.57], [0,0,0,0.43,0.57],[1,0,0,0,0],[0,1,0,0,0],[0,0,1,0,0],[0,0,0,1,0]])
    #PMatrix = np.array([[0,0,1], [1,0,0], [0,0,1], [0,0,1], [0,1,0]])
    #size_set = [9394592,9394592,9394592]
    size_set = [1,1,1,1,1]
    R = 16.0
    Isolated_Allocator(PMatrix,size_set,R);  # sys.argv[:2]


