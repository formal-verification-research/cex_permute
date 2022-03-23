
# DEPENDENCIES: 

# IVy with PDR enabled:
# http://microsoft.github.io/ivy/install

# PRISM Installation
# https://github.com/prismmodelchecker/prism

# PRISM API interface by Chris Winstead: 
# https://github.com/fluentverification/usu_importance_sampling


# Import functions from local files
# from asyncio import subprocess
# import subprocess
import getfiles as gf
import ivy
import prism_api
import commute
import pathProb
import os
# from subprocess import CalledProcessError, check_output

# Main procedure
if __name__ == "__main__":

  gf.cleanup()
  
  print(80*"*")
  print("Welcome to the counterexample permutation explorer.")
  print(80*"*")

  # Set ourselves up to start gathering probability
  pathP = pathProb.pathProb()

  # Get the names of files
  ivy_file = gf.get_ivy_file()
  prism_file = gf.get_prism_file()
  
  # Run ivy_check to get the seed counterexample
  print("Running ivy_check on your model...")
  # ivyresult = ivy.check(ivy_file)

  # Extract the transition path from the counterexample
  print("Finding the counterexample transition path...")
  ## ivy_path = ivy.get_path(ivyresult)
  with open("model.trace") as t:
    ivy_path = t.read() 

  # Save the path to a ivyfile.trace
  # with open(str(ivy_file).split(".")[0] + ".trace", 'w') as trace:
  with open("forprism.trace", 'w') as trace:
    trace.write(ivy_path)

  # TODO: There may be a way to remove transitions from a path.
  # That step should go here when it is developed.

  # Have the PRISM API walk along the path, reporting enabled transitions
  try:
    # recompile the java in case of updates
    os.system("make")
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
  commute.commute(ivy_path, intersection)
  
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
      pathP.readProbabilityFromString(api_result)

  print(80*"=")
  print("  Final Probability:", '%.10E' % pathP.prob)
  print(80*"=")
  print("Exiting without error.")
  print(80*"=")

  # Maybe joost-pieter's thing about storing a model instead of states
  
  # Modify the IVy model to exclude these paths (find an efficient way)
  
  # Repeat with the new IVy model

  gf.cleanup()
