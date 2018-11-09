import sys

def test(argv):
    x = int(argv[0]) + 1
    print x
    print x + 1



if __name__ == "__main__":
    test(sys.argv[1:])
