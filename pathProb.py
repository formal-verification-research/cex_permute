# Object to store the probability of a path
# Not in use at the moment.

class pathProb():

  # Initialize with probability 0
  def __init__(self):
    self.prob = 0.0

  # Multiply probability by x
  def accumulate(self, x):
    self.prob = self.prob * x

  # Read the probability from a string generated by SimulateModel.java
  def readProbabilityFromString(self, s):
    if "pathProbability " not in s:
      print(80*"*")
      print("ERROR: pathProbability not in input file:")
      print(s)
      print(80*"*")
    else:
      print()
      print("  Prior Probability:", '%.10E' % self.prob)
      newProb = float(s.split("pathProbability ")[1].strip())
      self.prob = self.prob + newProb
      print("  Path Probability: ", '%.10E' % newProb)
      print("  Total Probability:", '%.10E' % self.prob)
      print()
