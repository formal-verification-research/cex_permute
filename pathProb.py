
class pathProb():
  def __init__(self):
    self.prob = 0.0

  def accumulate(self, x):
    self.prob = self.prob * x

  def readProbabilityFromString(self, s):
    if "pathProbability " not in s:
      print("ERROR: pathProbability not in input file.")
    else:
      newProb = float(s.split("pathProbability ").strip()[1])
      self.prob = self.prob + newProb
      print("newProb", newProb, "self.prob", self.prob)
