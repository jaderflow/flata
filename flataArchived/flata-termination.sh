#! /bin/sh
#export LD_LIBRARY_PATH=/usr/local/lib:/usr/local/lib/jni
export CLASSPATH=./lib/flata.jar:./lib/nts.jar:./lib/antlr-3.3-complete.jar:./lib/glpk-java-4.47.jar
java verimag.main.Termination -t-merge-prec -t-fullincl $1

