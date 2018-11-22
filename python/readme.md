0. Dependency
	0.1: python 2.7+
	0.2: install numpy and cvxpy
		pip install numpy==1.14.4
		pip install cvxpy==0.4.9
1. OpuS
	1.1 call: python OpuS.py
	1.2 input: prefs.txt
		input format: cache budget \n user-0 pref \n user-1 pref \n ... 
	1.3 output: ratio_opus.txt (Cached ratio of files)
				factor_opus.txt (Access factor of users)
	1.4 dependency

2. FairRide
	2.1 call: python FairRide.py
	2.2 input: prefs.txt
		input format: cache budget \n user-0 pref \n user-1 pref \n ...
	2.3 output: ratio_fairride.txt (Cached ratio of files)
				factor_fairride.txt (Access factor of users)

3. Notes
	3.1 In OpuS, a user's access factor is the same for all files. In FairRide, a user's access factors for different files are not the same.
	3.2 To calculate the effecitve cache hit ratio: ratio * factor
