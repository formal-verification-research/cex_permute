import prism_api
import utils
import os
import tempfile
import ivy
import commute

def branch(orig_path, api_result, intersection, ivy_file, pathP):
  # set up a rolling list of transitions, with a flag to tell PRISM we want an
  # IVy model with the new "initial state" rather than a probability
  prefix_transitions = "CHANGE_IVY_INITIAL_STATE\t"

  # loop through the transitions, start from initial state and end before target
  for t in range(len(orig_path) - 1):
    # get next available transitions from list of available transitions
    available = prism_api.get_available(api_result, t)

    # clean up available transition list
    while "" in available:
      available.remove("")
    
    print("available transitions ", available)
    
    # don't want to consider the commuted transition
    for i in intersection:
      if i in available:
        available.remove(i)

    # don't want to consider an already-taken transition
    if orig_path[t] in available:
      available.remove(orig_path[t])
    
    # if you can't try any transition, move on
    if len(available) == 0:
      utils.printall("No available transitions remain after filtering. Moving on.")
      prefix_transitions = prefix_transitions + orig_path[t] + "\t"
      utils.printall("Prefix transitions now", prefix_transitions, "length", len(prefix_transitions.split("\t")))
      continue

    # for each available transition
    for av_tran in available:

      # make a new partial prefix trace
      new_prefix_trace = prefix_transitions + av_tran
      with open("forprism.trace", "w") as forprism:
        forprism.write(new_prefix_trace)
        print("new_prefix_trace", new_prefix_trace)
      
      # set up a temp file to catch results
      temp_result = tempfile.NamedTemporaryFile(mode="w+")

      # get the state from the prism simulation
      try:
        os.system("make test > " + temp_result.name)
      except:
        print("os.system Error, check main.py")
        continue
      
      # Go to the beginning of the temp result file and read it
      temp_result.seek(0)
      initial_state = temp_result.read()
      
      # with open("stepexplore.state") as state:
      #   initial_state = state.read()

      print("INITIAL STATE -------------------------------------")
      print(initial_state)
      print("END INITIAL STATE ---------------------------------")

      # set up a temp file to catch ivy_check results
      temp_log = tempfile.NamedTemporaryFile(mode="w+")
      # set up a temp file to provide a temporary ivy model
      temp_ivyfile = tempfile.NamedTemporaryFile(mode="w+")

      # Write a temporary IVy model with new initial state
      with open(ivy_file) as original_ivyfile:
        ivy.new_initial_state(initial_state, original_ivyfile, temp_ivyfile)

      # # write the new ivy model
      # with open(av_tran + ".log", "w") as new_ivyfile:
      #   pass
      # with open(av_tran + ".ivy", "w") as new_ivyfile:
      #   with open(ivy_file) as original_ivyfile:
      #     ivy.new_initial_state(initial_state, original_ivyfile, new_ivyfile)

      # get the ivy model with the new initial state from prism
      # new_ivyfile_name = av_tran + ".ivy"
      # print(new_ivyfile_name)

      # Check and parse the new IVy results
      ivy_path = ivy.check(temp_ivyfile.name)

      # print(new_ivy_result)
      # parse the new path
      # ivy_path = ivy.get_path(new_ivy_result)
      print("ivy_path", ivy_path)

      # Append the prefix for the path to go to prism
      forprism_path = new_prefix_trace.replace("CHANGE_IVY_INITIAL_STATE\t","") + "\t" + ivy_path
      print("forprism_path > ", forprism_path)
      
      # # Make the temp path file to feed into prism
      # temp_path = tempfile.NamedTemporaryFile(mode="w+")
      # temp_path.write(forprism_path)

      # write path for prism (file name required for now)
      with open("forprism.trace", 'w') as trace:
        trace.write(forprism_path)
      
      # Get the result from prism
      try:
        os.system("make test > " + temp_result.name)
      except:
        print("os.system Error in available transition trace! transition", av_tran)
        continue
      
      # Read in the file result
      temp_result.seek(0)
      api_result = temp_result.read()

      # api_result = result.read()
      # with open("prism.result") as result:
      #   api_result = result.read()

      # add in the probability
      pathP.readProbabilityFromString(api_result)

      # Commute after finding a new path
      api_result = prism_api.getEnabledTransitions(forprism_path)
      intersection = commute.commutePath(forprism_path, api_result, ivy_file, pathP)
      # input("PAUSE #1. PRESS ENTER TO KEEP GOING.")

    # add the transition to the list for the next time around
    prefix_transitions = prefix_transitions + orig_path[t] + "\t"
    utils.printall("Prefix transitions now", prefix_transitions)

    # quit()
    # input("Click enter to try from the next state")