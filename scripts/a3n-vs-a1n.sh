#!/bin/bash

# Runs one execution of main.A3NvsA1N 
# Please look at main.A3NvsA1N.java for the list of parameters


# compiles project
ant

# configures classpath and runs
classpath=.:bin:lib/*

echo "Launching experiment..."

java -classpath $classpath -Djava.library.path=lib/ main.A3NvsA1N "$@" 

echo "Done."
