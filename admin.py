import os

with open("BuildModel_Template.java", "r") as bm:
    template = bm.read()

for i in range(0,1):
    with open("src/BuildModel.java", "w") as bm:
        fl = template.replace("public static final int MAX_DEPTH = 0;", "public static final int MAX_DEPTH = " + str(i) + ";")
        bm.write(fl)
    os.system("mkdir reports/bryant/d" + str(i))
    os.system("time -o " + "reports/bryant/d" + str(i) + "/_totalTime.txt" + " python3 main.py >> reports/bryant/d" + str(i) + "/_report.txt")
    os.system("cat reports/bryant/time_*.txt > reports/bryant/d" + str(i) + "/_times.txt")
    os.system("mv reports/bryant/*.txt reports/bryant/d" + str(i) + "/")
    os.system("mv reports/bryant/*.sta reports/bryant/d" + str(i) + "/")
    os.system("mv reports/bryant/*.tra reports/bryant/d" + str(i) + "/")
    os.system("mv reports/bryant/*.lab reports/bryant/d" + str(i) + "/")