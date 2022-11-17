
import os

print("Start")

for i in range(-1, 25):
    folderName = "results/better_dupe/" + str(i)
    print(folderName)
    os.system("/usr/bin/time -o " + folderName  + "/prismTime.txt prism -importmodel " + folderName + "/prism.sta,tra,lab -ctmc scripts/pro2.csl > " + folderName + "/prismProb.txt")

for i in range(25, 105, 5):
    folderName = "results/higher_depth/" + str(i)
    print(folderName)
    os.system("/usr/bin/time -o " + folderName  + "/prismTime.txt prism -importmodel " + folderName + "/prism.sta,tra,lab -ctmc scripts/pro2.csl > " + folderName + "/prismProb.txt")

print("Finished")