import subprocess
import math
import os
import random
import sys

react = "2react"
timeUnits = "100"
tracePath = "paths/other/2trace_list.txt"
prop = "s2 >= 80"
recursion = "100"
directory = "2run" + recursion

options = open("options.txt", "w")
options.write(f"model models/{react}/model.sm\n")
options.write(f"trace {tracePath}\n")
options.write(f"property {prop}\n")
options.write(f"recursion bound {recursion}\n")
options.write("export both\n")
options.write("verbose")

options.close()

os.system(f"make")
os.system(f"mkdir results/{directory}")
os.system(f"/usr/bin/time -o results/{directory}/time.txt make test > results/{directory}/out.txt")
os.system(f"mv prism.* results/{directory}/")
os.system(f"mv storm.* results/{directory}/")
os.system(f"/usr/bin/time -o results/{directory}/prismTime.txt prism -importmodel results/{directory}/prism.tra,sta,lab -ctmc models/{react}/fullPro.csl > results/{directory}/prismOut.txt")



os.system(f"""/usr/bin/time -o results/{directory}/stormTime.txt storm --explicit results/{directory}/storm.tra results/{directory}/storm.lab --prop 'P=? [true U[0,{timeUnits}] "target"]' > results/{directory}/stormOut.txt""")
