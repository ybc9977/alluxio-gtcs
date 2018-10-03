#!/bin/bash

# $1 is the number of workers while $2 is the number of clients

python3 ~/Downloads/flintrock-master/standalone.py add-slaves gtcs --num-slaves $1+$2

python3 ~/Downloads/flintrock-master/standalone.py describe gtcs

