
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

  # Save the path to forprism.trace (name mandatory)
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
  # TODO: Does prism api read in the probabilities or the rates with getTransitionProbability???
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


  input("\n\nCLICK ENTER TO PROCEED TO 1-TRANSITION SYSTEM\n\n")

  # Force an extra enabled transition from each state, try to get a path
  # TODO add more heuristics here
  print("Finding novel paths by branching out 1 transition")
  # get the transitions in order
  orig_path = ivy_path.split("\t")
  # clean up the list
  while "" in orig_path:
    orig_path.remove("")
  # set up a rolling list of transitions, with a flag to tell PRISM we want an
  # IVy model with the new "initial state" rather than a probability
  prefix_transitions = "CHANGE_IVY_INITIAL_STATE\t"
  # loop through the transitions, start from initial state and end before target
  for t in range(len(orig_path) - 1):
    # get next available transitions from list of available transitions
    available = prism_api.get_available(api_result, t)
    # clean up available
    while "" in available:
      available.remove("")
    print("available transitions ", available)
    # exclusions (commented out for debug purposes)
    # # don't do the commuted transition
    # for i in intersection:
    #   if i in available:
    #     available.remove(i)
    # # don't do an already-taken transition
    # if orig_path[t] in available:
    #   available.remove(orig_path[t])
    # if you can't try a transition, move on
    if len(available) == 0:
      continue
    # for each available transition
    for av_tran in available:
      # make a new partial prefix trace
      new_prefix_trace = prefix_transitions + av_tran
      with open("forprism.trace", "w") as forprism:
        forprism.write(new_prefix_trace)
        print("new_prefix_trace", new_prefix_trace)
      # get the state from the prism simulation
      try:
        os.system("make test > stepexplore.state")
      except:
        print("os.system Error 832, check main.py")
        continue
      with open("stepexplore.state") as state:
        initial_state = state.read()
      print("INITIAL STATE -------------------------------------")
      print(initial_state)
      print("END INITIAL STATE ---------------------------------")
      # write the new ivy model
      with open(av_tran + ".ivy", "w") as new_ivyfile:
        with open(ivy_file) as old_ivyfile:
          ivy.new_initial_state(initial_state, old_ivyfile, new_ivyfile)
      # get the ivy model with the new initial state from prism
      new_ivy_result = ivy.check(av_tran + ".ivy")
      # parse the new path
      ivy_path = ivy.get_path(new_ivy_result)
      # get the probability from prism
      with open("forprism.trace", 'w') as trace:
        trace.write(ivy_path)
      try:
        os.system("make test > prism.result")
      except:
        print("os.system Error in available transition trace! transition", av_tran)
        continue
      # api_result = result.read()
      with open("prism.result") as result:
        api_result = result.read()
      # add in the probability
      pathP.readProbabilityFromString(api_result)
    # add the transition to the list for the next time around
    prefix_transitions = prefix_transitions + orig_path[t] + "\t"
    input("Click enter to try from the next state")
    






  print(80*"=")
  print("  Final Probability:", '%.10E' % pathP.prob)
  print(80*"=")
  print("Exiting without error.")
  print(80*"=")

  # Maybe joost-pieter's thing about storing a model instead of states
  
  # Modify the IVy model to exclude these paths (find an efficient way)
  
  # Repeat with the new IVy model

  gf.cleanup()
