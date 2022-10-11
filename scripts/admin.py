import os

with open("BuildModel_Template.java", "r") as bm:
    template = bm.read()

for i in range(0,1):
    with open("src/BuildModel.java", "w") as bm:
        fl = template.replace("public static final int MAX_DEPTH = 0;", "public static final int MAX_DEPTH = " + str(i) + ";")
        bm.write(fl)
<<<<<<< HEAD
    os.system("mkdir reports/lazy_sim")
    os.system("time -o " + "reports/lazy_sim/_totalTime.txt" + " python3 main.py > reports/lazy_sim/_report.txt")
    # os.system("cat reports/lazy_sim/time_*.txt > reports/bryant/d" + str(i) + "/_times.txt")
    # os.system("mv reports/bryant/*.txt reports/bryant/d" + str(i) + "/")
    # os.system("mv reports/bryant/*.sta reports/bryant/d" + str(i) + "/")
    # os.system("mv reports/bryant/*.tra reports/bryant/d" + str(i) + "/")
    # os.system("mv reports/bryant/*.lab reports/bryant/d" + str(i) + "/")
=======
    #os.system("mkdir reports/lazy_sim")
    os.system("time -o " + "reports/lazy_sim/_totalTime.txt" + " python3 main.py >> reports/lazy_sim/_report.txt")
    #os.system("cat reports/lazy_sim/time_*.txt > reports/bryant/d" + str(i) + "/_times.txt")
    #os.system("mv reports/bryant/*.txt reports/bryant/d" + str(i) + "/")
    #os.system("mv reports/bryant/*.sta reports/bryant/d" + str(i) + "/")
    #os.system("mv reports/bryant/*.tra reports/bryant/d" + str(i) + "/")
    #os.system("mv reports/bryant/*.lab reports/bryant/d" + str(i) + "/")
>>>>>>> 151e1f92627ce1a50dbc06a7f2c79fcc494a0470
