import os
import getfiles as gf
import ivy
import prism_api
import commute
import pathProb
import os

# Commute the discovered transitions (intersection) throughout the trace.
def commute(trace, intersection):
  with open("commuted.trace", 'w') as commuted:
    # Get individual moves
    t = trace.replace("\t\t", "\t").rstrip().lstrip().split("\t")
    # Remove any empty spots in t
    while "" in t:
      t.remove("")
    # Loop through every discovered transition in the intersection
    for i in intersection:
      # Loop through the whole array
      for a in range(0,len(t)):
        # Get the commuted transition's "prefix"
        for b in range(0,a):
          commuted.write(t[b] + "\t")
        # Write the commuted transition
        commuted.write(i + "\t")
        # Get the commuted transition's "suffix"
        for c in range(a,len(t)):
          commuted.write(t[c] + "\t")
        # Reset for next trace
        commuted.write("\n")


# Take in a string path from IVy, commute, and give back a total probability.
def commutePath(ivy_path, pathP):
  print("commutePath initialized.")
  print(">>", ivy_path.replace("\t"," "))
  # Save the path to forprism.trace (name mandatory)
  with open("forprism.trace", 'w') as trace:
    trace.write(ivy_path)

  # Have the PRISM API walk along the path, reporting enabled transitions
  try:
    # recompile the java in case of updates
    os.system("make test > prism.result")
    # using depreciated os.system because subprocess was not working
    # result = subprocess.check_output(['make','test'])
  except:
    print("os.system Error!")
    quit()

  # api_result = result.read()
  with open("prism.result") as result:
    api_result = result.read()

  # add in the probability
  # TODO: Does prism api read in the probabilities or the rates with getTransitionProbability???
  print("Prism Returns the first time:")
  pathP.readProbabilityFromString(api_result)

  # Find the intersection of all enabled transitions
  intersection = prism_api.get_intersection(api_result)
  
  # for some reason, blanks keep appearing in the intersection
  while "" in intersection:
    intersection.remove("")
  print("Path Intersection:", intersection)

  # check if intersection is empty
  if len(intersection) < 1:
    print("No intersections found. Bummer.")
    print("Single path probability:", pathP)
    print("Exiting without errors.")
    quit()
    
  # Build paths with the enabled transitions commuted
  commute(ivy_path, intersection)
  
  # Find out if t_alpha will get you to a target state
  # Maybe just do this in the Java script... whatever's more efficient
  for t_alpha in intersection:
    newpath = ivy_path + " " + t_alpha
    with open("forprism.trace", "w") as temp_path:
      temp_path.write(newpath)
    try:
      os.system("make test > prism.result")
    except:
      print("os.system Error in commuted trace!")
      continue
    with open("prism.result") as result:
      if "ERR_TAR_NO_RCH" in result.read():
        print("Commuting", t_alpha, "does not lead to a target state.")
        intersection.remove(t_alpha)
        print("Removed", t_alpha, "from inspection.")
      else:
        print("Commuting", t_alpha, "leads to a target state :)")


  # Go through the traces, accumulate probability
  # This may be better to do in Java actually...
  with open("commuted.trace") as commuted:
    for trace in commuted:
      with open("forprism.trace", "w") as forprism:
        # Isolate one trace at a time to send to the api
        forprism.write(trace.rstrip())
      try:
        # os.system("make") # may be needed, idk why though
        os.system("make test > prism.result")
      except:
        print("os.system Error in commuted trace!")
        continue
      # Read the results back in
      # TODO eventually add some amount of recursion here to 
      # commute in the new traces
      with open("prism.result") as result:
        api_result = result.read()
      # add in the probability
      print("Prism Returns the second time:")
      pathP.readProbabilityFromString(api_result)

  return intersection