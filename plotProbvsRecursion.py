import subprocess
import math
import os
import random
import sys

react = "2react"
recursion = "1"
directory = "time8Run"
rootdir = '/cygdrive/c/Users/adminstrator/cex_permute/results/2reactDepth'

depth = []
prob = []
time = []
states = []
tran = []
count1 = 0
for subdir, dirs, files in os.walk(rootdir):
    for file in files:
        if file == "prismOut.txt":
            #print(file)
            depth.append(int(subdir[72:75]))
            with open(subdir + "/prismOut.txt", "r") as f:
                count = 0
                while True:
                    line = f.readline()
                    if not line:
                        break
                    if line[0:6] == "Result":
                        #print(float(line.split() [1]))
                        prob.append(float(line.split() [1]))
                    #if line[0] == "N":
        elif file == "time.txt":
            with open(subdir + "/time.txt", "r") as f:
                count = 0
                while True:
                    count += 1
                    line = f.readline()
                    if not line:
                        break
                    if count == 1:
                        #print(float(line[9:13]))
                        time.append(float(line[9:13]))

        elif file == "out.txt":
            count1 += 1
            with open(subdir + "/out.txt", "r") as f:
                #count = 0
                while True:
                    #count += 1
                    line = f.readline()
                    #print(line.split() [1])
                    if not line:
                        break
                    if len(line.split()) > 1:
                        if (line.split() [1]) == "states":
                            #print(int(line[0:1]))
                            states.append(int(line.split()[0]))
                        elif (line.split() [1]) == "transitions":
                            #print(int(line[0:1]))
                            tran.append(int(line.split()[0]))
                    #if line[0] == "N":
            #print(subdir[72:75])

#print(count1)
print(time)
print(depth)
print(tran)
print(states)
#print(prob)




"""
with open("reaction_list.txt", "r") as f:
    count = 0
    while True:
        line = f.readline()
        if not line:
            break
        if line[0] == "N":
            Totaltran = Totaltran + int(line[23:26])
            Totaltranlist.append(int(line[23:26]))
        elif line[0] == "I":
            Totaliter = Totaliter + int(line[38:41])
            Totaliterlist.append(int(line[38:41]))
        for x in range(numOfReactions):
            stringnum = str(x+1)
            stringreact = "r" + stringnum
            if line[0:2] == stringreact:
                if len(line) == 17:
                    Total[x] = Total[x] + int(line[14:16])
                    iterations[x].append(int(line[14:16]))
                elif len(line) == 16:
                    Total[x] = Total[x] + int(line[14])
                    iterations[x].append(int(line[14]))

options = open("options.txt", "w")
options.write(f"model models/{react}/model.sm\n")
options.write(f"trace {tracePath}\n")
options.write(f"property {prop}\n")
options.write(f"recursionBound {recursion}\n")
options.write(f"timeBound {timeUnits}\n")
options.write(f"flexibility {flex}\n")
options.write(f"terminate time\n")
options.write(f"cycleLength 0\n")
options.write("export both\n")
options.write("verbose")

options.close()

os.system(f"make")
os.system(f"mkdir results/{directory}")
os.system(f"/usr/bin/time -o results/{directory}/time.txt make test > results/{directory}/out.txt")
os.system(f"mv prism.* results/{directory}/")
os.system(f"mv storm.* results/{directory}/")"""
#os.system(f"/usr/bin/time -o results/{directory}/prismTime.txt prism -importmodel results/{directory}/prism.tra,sta,lab -ctmc models/{react}/fullPro.csl > results/{directory}/prismOut.txt")
#os.system(f"""/usr/bin/time -o results/{directory}/stormTime.txt storm --explicit results/{directory}/storm.tra results/{directory}/storm.lab --prop 'P=? [true U[0,{timeUnits}] "target"]' > results/{directory}/stormOut.txt""")
#os.system(f"cp options.txt results/{directory}/")
