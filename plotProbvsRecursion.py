import subprocess
import math
import os
import random
import sys
import matplotlib.pyplot as plt
#from matplotlib import rc, rcParams
from matplotlib.backends.backend_pdf import PdfPages
#import numpy as np

plt.rcParams["font.family"] = "Arial"
plt.rcParams["font.size"] = "18"
plt.rcParams["mathtext.fontset"] = "cm"
plt.rcParams["axes.labelweight"] = "bold"

react = "2react"
recursion = "1"
directory = "time8Run"
rootdir = '/cygdrive/c/Users/adminstrator/cex_permute/results/2reactDepth'

depth = []
prob = []

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
            #print(subdir[72:75])

print(depth)
print(prob)


plt.plot(depth, prob, "o", color='teal', linewidth=3.5)
plt.plot(depth, prob, "", color='teal', linewidth=2.5)

#plt.title("Concurrent Path Builder Preliminary Result")

plt.ylabel('Path Probability', fontsize=18)
plt.yscale("log")

# Setting the interval of ticks of y-axis to 10.
#listOf_Yticks = np.arange(1E-100, 1E-20, 1E10)
listOf_Yticks = [1e-100,1e-90,1e-80,1e-70,1e-60,1e-50,1e-40,1e-30,1e-20]
plt.yticks(listOf_Yticks)

listOf_Xticks = [4,8,12,16]
plt.xticks(listOf_Xticks)

plt.xlabel('Recursion depth', fontsize=18)

plt.grid(color='gainsboro', axis='y')

plt.tight_layout()

plt.savefig('plot.png')

with PdfPages(r'plot.pdf') as export_pdf:
    export_pdf.savefig()

plt.show()
        



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
