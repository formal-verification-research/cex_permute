import os
import getfiles as gf
import ivy
import prism_api
import commute
import pathProb
import os
import tempfile
import utils
import branch

MAX_DEPTH = 1

# Merged into commutePath to help with some logistics
# Commute the discovered transitions (intersection) throughout the trace.
# def commute(trace, intersection):
#   with open("commuted.trace", 'w') as commuted:
#     # Get individual moves
#     t = trace.replace("\t\t", "\t").rstrip().lstrip().split("\t")
#     # Remove any empty spots in t
#     while "" in t:
#       t.remove("")
#     # Loop through every discovered transition in the intersection
#     for i in intersection:
#       # Loop through the whole array
#       for a in range(0,len(t)):
#         # Get the commuted transition's "prefix"
#         for b in range(0,a):
#           commuted.write(t[b] + "\t")
#         # Write the commuted transition
#         commuted.write(i + "\t")
#         # Get the commuted transition's "suffix"
#         for c in range(a,len(t)):
#           commuted.write(t[c] + "\t")
#         # Reset for next trace
#         commuted.write("\n")


# Take in a string path from IVy, commute, and give back a total probability.
def commutePath(ivy_path, api_result, ivy_file, pathP, depth=0):
  print("commutePath initialized.")
  print(">>", ivy_path.replace("\t"," "))

  # check if intersection is empty
  if depth == MAX_DEPTH:
    print("Max recursion depth reached.")
    return []

  temp_result = tempfile.NamedTemporaryFile(mode="w+")
  print("NEW FILE COMMUTE1")

  # add in the probability
  print("Prism Returns the first time during this function call:")
  pathP.readProbabilityFromString(api_result)

  # Find the intersection of all enabled transitions
  intersection = prism_api.get_intersection(api_result)

  utils.printall("Branching")
  utils.printall("ivy_path", ivy_path)
  utils.printall("api_result", api_result)
  utils.printall("intersection", intersection)
  utils.printall("ivy_file", ivy_file)
  utils.printall("pathP.prob", pathP.prob)
  
  branch.branch(ivy_path, api_result, intersection, ivy_file, pathP, depth)
  
  # for some reason, blanks keep appearing in the intersection
  while "" in intersection:
    intersection.remove("")
  print("Path Intersection:", intersection)

  # check if intersection is empty
  if len(intersection) < 1:
    print("No intersections found.")
    print("Single path probability:", pathP.prob)
    return intersection
    # quit()
  
  # Find out if t_alpha will get you to a target state
  # Maybe just do this in the Java script... whatever's more efficient
  for t_alpha in intersection:
    newpath = ivy_path + " " + t_alpha
    with open("forprism.trace", "w") as temp_path:
      temp_path.write(newpath)
    try:
      os.system("make test > " + temp_result.name)
      # os.system("make test > prism.result")
    except:
      print("os.system Error in commuted trace!")
      continue

    temp_result.seek(0)

    if "ERR_TAR_NO_RCH" in temp_result.read():
      print("Commuting", t_alpha, "does not lead to a target state.")
      intersection.remove(t_alpha)
      print("Removed", t_alpha, "from inspection.")
    else:
      print("Commuting", t_alpha, "leads to a target state :)")

  # Build paths with the enabled transitions commuted
  # commute(ivy_path, intersection)

  commuted_paths = tempfile.NamedTemporaryFile(mode="w+")
  print("NEW FILE COMMUTE2")

  # Get individual moves
  t = ivy_path.replace("\t\t", "\t").rstrip().lstrip().split("\t")
  # Remove any empty spots in t
  while "" in t:
    t.remove("")
  # Loop through every discovered transition in the intersection
  for i in intersection:
    # Loop through the whole array
    for a in range(0,len(t)):
      # Get the commuted transition's "prefix"
      for b in range(0,a):
        commuted_paths.write(t[b] + "\t")
      # Write the commuted transition
      commuted_paths.write(i + "\t")
      # Get the commuted transition's "suffix"
      for c in range(a,len(t)):
        commuted_paths.write(t[c] + "\t")
      # Reset for next trace
      commuted_paths.write("\n")

  commuted_paths.seek(0)

  # Go through the traces, accumulate probability
  # This may be better to do in Java actually...
  # with open("commuted.trace") as commuted:
  for commuted_path in commuted_paths:
    with open("forprism.trace", "w") as forprism:
      # Isolate one trace at a time to send to the api
      forprism.write(commuted_path.rstrip())
    try:
      # os.system("make") # may be needed, idk why though
      os.system("make test > " + temp_result.name)
    except:
      print("os.system Error in commuted trace!")
      continue
    # Read the results back in
    # TODO eventually add some amount of recursion here to 
    # commute in the new traces
    temp_result.seek(0)
    # api_result = temp_result.read()

    utils.printall("Commuting")

    api_result = prism_api.getEnabledTransitions(commuted_path)
    nested_intersection = commute.commutePath(commuted_path, api_result, ivy_file, pathP, depth + 1)

    # add in the probability -- removed due to recursive function call instead
    # print("Prism Returns the second time:")
    # print("trace >>", trace)
    # pathP.readProbabilityFromString(api_result)
  
  temp_result.close()
  print("CLOSED FILE COMMUTE1")

  commuted_paths.close()
  print("CLOSED FILE COMMUTE2")


  return intersection