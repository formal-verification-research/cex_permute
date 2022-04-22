
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

  # TODO Check if we're doing pass by reference
  commute.commutePath(ivy_path, pathP)
  # pathP = commute.commutePath(ivy_path, pathP)

  # input("\n\nCLICK ENTER TO PROCEED TO 1-TRANSITION SYSTEM\n\n")

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
    # don't do the commuted transition
    for i in intersection:
      if i in available:
        available.remove(i)
    # don't do an already-taken transition
    if orig_path[t] in available:
      available.remove(orig_path[t])
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
      with open(av_tran + ".log", "w") as new_ivyfile:
        pass
      with open(av_tran + ".ivy", "w") as new_ivyfile:
        with open(ivy_file) as old_ivyfile:
          ivy.new_initial_state(initial_state, old_ivyfile, new_ivyfile)
      # get the ivy model with the new initial state from prism
      new_ivyfile_name = av_tran + ".ivy"
      print(new_ivyfile_name)
      new_ivy_result = ivy.check(new_ivyfile_name)
      # print(new_ivy_result)
      # parse the new path
      ivy_path = ivy.get_path(new_ivy_result)
      print("ivy_path", ivy_path)
      forprism_path = new_prefix_trace.replace("CHANGE_IVY_INITIAL_STATE\t","") + "\t" + ivy_path
      print("forprism_path > ", forprism_path)
      # get the probability from prism
      with open("forprism.trace", 'w') as trace:
        trace.write(forprism_path)
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
      # input("PAUSE #1. PRESS ENTER TO KEEP GOING.")
    # add the transition to the list for the next time around
    prefix_transitions = prefix_transitions + orig_path[t] + "\t"
    # input("Click enter to try from the next state")
    






  print(80*"=")
  print("  Final Probability:", '%.10E' % pathP.prob)
  print(80*"=")
  print("Exiting without error.")
  print(80*"=")

  # Maybe joost-pieter's thing about storing a model instead of states
  
  # Modify the IVy model to exclude these paths (find an efficient way)
  
  # Repeat with the new IVy model

  gf.cleanup()
