import os

'''
BENCHMARKING TASKS:

Models under test:
    - Yeast Polarization (8-reaction)
    - Futile Cycle (6-reaction)
    - Simple Cycle (2-reaction)
    - The new one (12-reaction)

Tasks:
1. Get a shortest seed trace and one other non-shortest seed trace for each model
2. Get true probability estimates for as many models as we can (prism model.sm pro.csl)
3. Run combinatorics of benchmarks (basically nest for-loops through each of the arrays) on each model

This will write the options.txt file for each combination (which can then be copied in-order to 
your working directory and tested).

The arrays below might need to be adjusted based on each model. 
The values here are optimized for the 8-reaction model.

'''

# EDIT THE FOLLOWING FOR EACH MODEL
shortModelName = "8reaction" #for file name generation
modelFile = "models/8react/model.sm"
traceFile = "paths/other/a.txt" #ideally this contains both traces (see step 1 above) separated by a newline
cslProperty = "G_bg = 50"
modelTimeBound = 20 
export = "prism" #change to "storm" or "both" if you need to, but we should just be OK to check prism
verbose = "false"

# for the termination based on depth
recursionBounds = [0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 16, 20, 25, 30]

# for the termination based on time (remember to input the model's time bound)
flexibilities = [0.01, 0.02, 0.03, 0.04, 0.05, 0.06, 0.07, 0.08, 0.09, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2]

# for cycle addition
cycleLengths = [0, 2, 4, 6, 8]

# tolerance for removing probability sinks
removeTolerances = [0.0, 0.001, 0.01, 0.05, 0.1, 0.3, 0.5, 0.7, 0.9, 0.91, 0.93, 0.95, 0.97, 0.98, 0.99, 0.999]


# build the default case
optionsString = "model " + modelFile + "\n"
optionsString = optionsString + "trace " + traceFile + "\n"
optionsString = optionsString + "property " + cslProperty + "\n"
optionsString = optionsString + "export " + export + "\n"
optionsString = optionsString + "verbose " + verbose + "\n"

# run the tests
for cycleLength in cycleLengths:
    for removeTolerance in removeTolerances:
        
        # first test recursion depth termination
        for recursionBound in recursionBounds:
            optionsFile = optionsString + ("recursionBound %d\ncycleLength %d\nremoveTolerance %1.4f" % (recursionBound, cycleLength, removeTolerance))
            fileName = shortModelName + ("_cyc%d_tol%1.4f_depth%d" % (cycleLength, removeTolerance, recursionBound))
            with open(fileName + "_options.txt", "w") as thisOptions:
                thisOptions.write(optionsFile)
            with open("options.txt", "w") as genOptions:
                genOptions.write(optionsFile)
            timeFileName = fileName + "_time.txt"
            commuteFileName = fileName + "_commute.txt"
            prismFileName = fileName + "_prism.txt"
            os.system("echo REPLACE ME IN THE CODE") #replace this with something like the next lines
            # /usr/bin/time -o {timeFileName} make test > {commuteFileName}; 
            # mv prism.tra {fileName}.tra
            # mv prism.sta {fileName}.sta
            # mv prism.lab {fileName}.lab
            # prism -importmodel {fileName}.tra,sta,lab -ctmc {propertyFile.csl} > {prismFileName}

        # then test time-based termination flexibility
        for flexibility in flexibilities:
            optionsFile = optionsString + ("flexibility %1.4f\ncycleLength %d\nremoveTolerance %1.4f" % (flexibility, cycleLength, removeTolerance))
            fileName = shortModelName + ("_cyc%d_tol%1.4f_flex%1.4f" % (cycleLength, removeTolerance, flexibility))
            with open(fileName + "_options.txt", "w") as thisOptions:
                thisOptions.write(optionsFile)
            with open("options.txt", "w") as genOptions:
                genOptions.write(optionsFile)
            timeFileName = fileName + "_time.txt"
            commuteFileName = fileName + "_commute.txt"
            prismFileName = fileName + "_prism.txt"
            os.system("echo REPLACE ME IN THE CODE") #replace this with your list of commands to run on a model


