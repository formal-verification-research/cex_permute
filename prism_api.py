# Interact with the PRISM API

import os

# Get the intersection given a trace string
def get_intersection(trace): # Returns ARRAY

  # Get rid of brackets and set up strings to be manipulated
  trace_a = trace.replace("]","").replace("[","")
  trace = ""

  # Store the previous and current enabled transitions
  last_enabled = []
  curr_enabled = []

  # Clean up after the binary file
  # TODO make a better binary file
  for line in trace_a.split("\n"):
    if "bin" not in line and "length" not in line:
      trace = trace + line + "\n"
  
  # Current enabled transitions are initialized as the first line
  curr_enabled = trace.split("\n")[0].split(" ")

  # Loop through, comparing side-by-side to find the intersection
  for line in trace.split("\n"):
    # Find the end of the file
    if ">>" in line:
      break
    if "pathProbability " in line:
      print("path probability from get_intersection: " + line.split("pathProbability ")[1])
      break
    # Push previous enabled transitions back to last_enabled
    last_enabled = curr_enabled
    # Get the new current enabled transitions, adding only
    #  the transitions that match the previous enabled transitions
    curr_enabled = []
    for transition in line.split(" "):
      if transition in last_enabled:
        curr_enabled.append(transition)
  
  # Remove blanks from current enabled transitions
  while ("" in curr_enabled):
    curr_enabled.remove("")

  # Return the ARRAY of transitions enabled at every state
  return curr_enabled


# Get all available transitions from the PRISM API result
def get_available(trace, index): # Returns ARRAY

    # Set up strings for manipulation
    trace_a = trace.replace("]","").replace("[","")
    trace = ""

    # Clean up after the binary file
    # TODO make a better binary file
    for line in trace_a.split("\n"):
        if "bin" not in line and "length" not in line and ">>" not in line and "pathProbability" not in line:
            trace = trace + line + "\n"
    
    # Get the line at (index) for available transitions
    available = trace.split("\n")[index].split(" ")
    
    # Return the ARRAY of available transitions
    return available


# Get the enabled transitions from the PRISM API
def getEnabledTransitions(ivy_path): # Returns STRING

    # Save the path to forprism.trace (name mandatory)
    with open("forprism.trace", 'w') as trace:
        trace.write(ivy_path)

    # Have the PRISM API walk along the path, reporting enabled transitions
    try:
        # recompile the java in case of updates
        os.system("make test > prism.result")
        # using depreciated os.system because subprocess was not working
    except:
        print("os.system Error in prism_api.py function getEnabledTransitions() when calling make test!")
        quit()

    # Using file handling because subprocess was not working
    with open("prism.result") as result:
        return result.read()


# Function to build a PRISM model given a path and its commutable transitions
def buildmodel():
  try:
    os.system("make test")
  except:
    print("os.system Error in prism_api.py function buildModel() when calling make test!")
    return