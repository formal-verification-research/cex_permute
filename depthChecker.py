import os

with open("bmTemplate.txt", "r") as old:
    oldStr = old.read()

for i in range(-1,25):
        with open("src/BuildModel.java", "w") as new:
            print("on phase " + str(i))
            new.write(oldStr.replace("public static final int MAX_DEPTH = 0;", "public static final int MAX_DEPTH = " + str(i) + ";"))
            os.system("make > /dev/null 2>&1")
            fldr = "timed_recdepth/" + str(i)
            os.system("mkdir " + fldr + " > /dev/null 2>&1")
            os.system("/usr/bin/time -o" + fldr + "/time.txt make test > " + fldr + "/result.txt")
            os.system("mv prism.lab prism.sta prism.tra storm.lab storm.tra " + fldr)