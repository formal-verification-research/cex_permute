
class pathProb():
  def __init__(self):
    self.prob = 0.0

  def accumulate(self, x):
    self.prob = self.prob * x

  def readProbabilityFromString(self, s):
    if "pathProbability " not in s:
      print("ERROR: pathProbability not in input file.")
    else:
      print("Prior Probability:", newProb)
      newProb = float(s.split("pathProbability ")[1].strip())
      self.prob = self.prob + newProb
      print("Path Probability: ", newProb)
      print("Total Probability:", self.prob)
