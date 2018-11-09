import numpy
import subprocess
import time
import pickle


def simulation():


    log =open("simulator.log", "a")




    #ProbMatrix = numpy.array([[ 0.03481625,0.03675048,0.40038685,0.05029014,0.03675048,0.11218569,0.06576402,0.1450677,0.04642166,0.07156673],
    #                [ 0.06150794,0.0515873,0.05357143,0.18055556,0.03571429,0.03174603,0.06944444,0.11507937,0.35515872,0.04563492],
    #                [ 0.0341556,0.05502846,0.0341556,0.12523719,0.04174573,0.17267552,0.03795066,0.35294118,0.07590133,0.07020873],
    #                [ 0.02994012,0.1756487,0.10578842,0.05588822,0.05788423,0.0259481,0.38522954,0.05588822,0.08782435,0.01996008]])

    #ProbMatrix = numpy.array([[0.2,0.2,0.3,0.3,0,0], [0,0,0.3,0.3,0.2,0.2]])
    #ProbMatrix_fake =  numpy.array([[0.33,0.33,0.17,0.17,0,0], [0,0,0.3,0.3,0.2,0.2]])
    #ProbMatrix = numpy.array([[0.43,0,0,0,0.57], [0,0.43,0,0,0.57],[0,0,0.43,0,0.57], [0,0,0,0.43,0.57],[1,0,0,0,0],[0,1,0,0,0],[0,0,1,0,0],[0,0,0,1,0]])
    #ProbMatrix_fake = numpy.array([[0.8,0,0,0,0.2], [0,0.43,0,0,0.57],[0,0,0.43,0,0.57], [0,0,0,0.43,0.57],[1,0,0,0,0],[0,1,0,0,0],[0,0,1,0,0],[0,0,0,1,0]])
    #ProbMatrix = numpy.array([[1, 0, 0], [0.45, 0.55, 0], [0, 0.55, 0.45], [0, 0.55, 0.45]])
    #ProbMatrix_fake = numpy.array([[1, 0, 0], [0.55, 0.45, 0], [0, 0.55, 0.45], [0, 0.55, 0.45]])
    with open('pm1.pickle') as f:  # Python 3: open(..., 'rb')
        ProbMatrix = pickle.load(f)

    n = ProbMatrix.shape[0]  # number of users
    m = ProbMatrix.shape[1]  # number of files
    ResponseTimeList = [[] for i in range(n)]

    for i in range(0, n):
        ProbMatrix[i,m-1] = 1.0 - sum(ProbMatrix[i,range(0,m-1)])
        print sum(ProbMatrix[i,:])


    # User 1 -n start to submit file request, each user with the same frequency

    for token in range(0,n*1000):
        user_id = token % n
        file_id = numpy.random.choice(numpy.arange(0, m), p=ProbMatrix[user_id,:])
        start = time.time()
        cmd = ["alluxio", "fs", "copyToLocal", "/sample/sample-%s"%(file_id+1), "~/","%s"%(user_id+1)]
        print cmd
        #p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        while(True):
            retcode = p.poll() #returns None while subprocess is running
            line = p.stdout.readline()
            print line
            if(retcode is not None):
                break
        end = time.time()
        response_time = end - start
        print "Response_time : %s\n" % response_time
        ResponseTimeList[user_id].append(response_time)
    '''

    # User 1 starts to cheat
    for token in range(0,n*200):
        user_id = token % n
        file_id = numpy.random.choice(numpy.arange(0, m), p=ProbMatrix_fake[user_id,:])
        start = time.time()
        cmd = ["alluxio", "fs", "copyToLocal", "/sample/sample-%s"%(file_id+1), "~/","%s"%(user_id+1)]
        print cmd
        #p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        while(True):
            retcode = p.poll() #returns None while subprocess is running
            line = p.stdout.readline()
            print line
            if(retcode is not None):
                break
        end = time.time()
        response_time = end - start
        ResponseTimeList[user_id].append(response_time)
    '''
    '''
    # User 1 starts to cheat by tripling his access to the file that only he himself wants (file 0 and file 1)
    for token in range(0,400):
        user_id = token % 2
        file_id = numpy.random.choice(numpy.arange(0, m), p=ProbMatrix[user_id,:])
        start = time.time()
        cmd = ["alluxio", "fs", "copyToLocal", "/sample/sample-%s"%(file_id+1), "~/","%s"%(user_id+1)]
        print cmd
        #p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        while(True):
            retcode = p.poll() #returns None while subprocess is running
            line = p.stdout.readline()
            print line
            if(retcode is not None):
                break
        if user_id == 0 and (file_id == 0 or file_id ==1): # repeat twice
            p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            while(True):
                retcode = p.poll() #returns None while subprocess is running
                line = p.stdout.readline()
                print line
                if(retcode is not None):
                    break
            p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            while(True):
                retcode = p.poll() #returns None while subprocess is running
                line = p.stdout.readline()
                print line
                if(retcode is not None):
                    break
            end = time.time()
            response_time = (end - start) / 3  # average
            ResponseTimeList[user_id].append(response_time)
        else:
            end = time.time()
            response_time = end - start
            ResponseTimeList[user_id].append(response_time)
    '''

    # write the response time
    for i in range(0, n):
        log.write("User %s\t" % (i+1))
        for response_time in ResponseTimeList[i]:
            log.write("%s\t" % response_time)
        log.write("\n")
    log.write("\n\n\n")
    log.close()




if __name__ == "__main__":
    simulation()
