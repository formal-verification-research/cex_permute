
import matplotlib.pyplot as plt
#from matplotlib import rc, rcParams
#from matplotlib.backends.backend_pdf import PdfPages
#import numpy as np

prob = []
pi = 0
currprob = []
maxlen = 0

with open("_report.txt", "r") as report:
  for line in report:
    if "===========" in line:
      prob.append(currprob)
      if len(currprob) > maxlen:
        maxlen = len(currprob)
      pi = pi + 1
      currprob = []
      continue
    if "tr r" not in line:
      continue
    getpathprob = line.split(" path ")[1]
    currprob.append(float(getpathprob))

#print(str(prob))

#x_axis = []

#for i in range(maxlen):
  #x_axis.append(i)


plt.yscale("log")
plt.xscale("linear")

for p in prob:
  x_axis = []
  for i in range(len(p)):
    x_axis.append(i)
  plt.plot(x_axis, p)

plt.show()

















##plt.rcParams["font.weight"] = "bold"
#plt.rcParams["font.family"] = "Arial"
#plt.rcParams["font.size"] = "18"
#plt.rcParams["mathtext.fontset"] = "cm"
#plt.rcParams["axes.labelweight"] = "bold"

#z = [1.527, 1.870, 2.098, 4.758, 18.294]

#x = [1, 2, 3, 4, 5]

#y = [3.5596055075975633E-97, 1.7951858408067537E-67, 5.5907850924339506E-42, 1.979841075573553E-33, 1.7992544067761122E-22]

#plt.plot(z, y, "o", color='teal', linewidth=3.5)
#plt.plot(z, y, "", color='teal', linewidth=2.5)

##plt.title("Concurrent Path Builder Preliminary Result")

#plt.ylabel('Path Probability', fontsize=18)
#plt.yscale("log")

## Setting the interval of ticks of y-axis to 10.
##listOf_Yticks = np.arange(1E-100, 1E-20, 1E10)
#listOf_Yticks = [1e-100,1e-90,1e-80,1e-70,1e-60,1e-50,1e-40,1e-30,1e-20]
#plt.yticks(listOf_Yticks)

#listOf_Xticks = [4,8,12,16]
#plt.xticks(listOf_Xticks)

#plt.xlabel('Execution Time (s)', fontsize=18)

#plt.grid(color='gainsboro', axis='y')

#plt.tight_layout()

#plt.savefig('plot.png')

#with PdfPages(r'plot.pdf') as export_pdf:
    #export_pdf.savefig()

#plt.show()
