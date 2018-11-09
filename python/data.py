
import pickle

obj = 1

# Saving the objects:
with open('pm.pickle', 'w') as f:
    pickle.dump(obj, f)

# Getting back the objects:
with open('pm.pickle') as f:  # Python 3: open(..., 'rb')
    ProbMatrix = pickle.load(f)
