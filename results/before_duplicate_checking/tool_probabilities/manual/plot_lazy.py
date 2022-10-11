import matplotlib.pyplot as plt

# prob[seed][rec_depth]

prob = []

for f in range(0,77):
  pathprob = []
  for d in range(0,5):
    filename = "d" + str(d) + "/sm_" + str(f) + ".txt"
    with open(filename, "r") as sm:
      resultline = ""
      for line in sm:
        if "Result: " in line:
          resultline = line
          break
    sp = resultline.split(": ")[1]
    p = float(sp)
    pathprob.append(p)
  prob.append(pathprob)

x_axis = [0,1,2,3,4]

plt.yscale("log")

for seed in prob:
  plt.plot(x_axis, seed)

plt.show()
