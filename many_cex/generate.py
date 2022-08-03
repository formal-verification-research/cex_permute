class Node:
  
  def __init__(self, pathTo, data, r5_avail, pathLen):
    self.data = data
    self.pathTo = pathTo + data + "\t"
    self.children = []
    if pathLen < 100:
      if r5_avail < 50:
        self.children.append(Node(self.pathTo, "r1", r5_avail+1, pathLen+1))
      if r5_avail > 0:
        self.children.append(Node(self.pathTo, "r5", r5_avail-1, pathLen+1))
      
  def printPaths(self):
    wholeStr = ""
    if len(self.children) > 0:
      for child in self.children:
        wholeStr = wholeStr + child.printPaths() + "\n"
    else:
      return self.pathTo
    return wholeStr



with open("test.txt", "w") as t:
  tNode = Node("", "r1", 1, 0)
  t.write(tNode.printPaths())