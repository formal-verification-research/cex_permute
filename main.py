
# DEPENDENCIES: 

# IVy with PDR enabled:
# http://microsoft.github.io/ivy/install

# PRISM API interface by Chris Winstead: 
# https://github.com/fluentverification/usu_importance_sampling

# Issue: package parser not found
# [ npm install java-parser --save-dev ] was not helpful
# Issue mostly resolved.
# TODO: Add issue resolution description to issue tracker

# Parser also included here:
# https://github.com/prismmodelchecker/prism-ext/tree/master/prism/src/prism

# Import functions from local files
# from asyncio import subprocess
import subprocess
import getfiles as gf
import ivy
import prism_api
import commute

import os

from subprocess import CalledProcessError, check_output

# Main procedure
if __name__ == "__main__":

  gf.cleanup()
  
  print(80*"*")
  print("Welcome to the counterexample permutation explorer.")
  print(80*"*")

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
  with open(str(ivy_file).split(".")[0] + ".trace", 'w') as trace:
    trace.write(ivy_path)

  # TODO: There may be a way to remove transitions from a path.
  # That step should go here when it is developed.

  # Have the PRISM API walk along the path, reporting enabled transitions
  ## api_result = check_output(['java', 'temp'])

  try:
    os.system("make")
    os.system("make test > model.result")
    # result = subprocess.check_output(['make','test'])
    # result = subprocess.check_output(['make','test'])
  except:
    print("os.system Error!")
    quit()

  # api_result = result.read()
  with open("model.result") as result:
    api_result = result.read()

  # Find the intersection of all enabled transitions
  intersection = prism_api.get_intersection(api_result)  

  print(intersection)
  
  # Iteratively find the intersection rather than getting them all
  # This feature may be complicated to add because we call java

  # Build paths with the enabled transitions commuted
  commute.commute(ivy_path, intersection)

  # TODO Check the permuted paths with the prism api somehow

  # Maybe joost-pieter's thing about storing a model instead of states

  # Test the commuted paths in PRISM (for dev stage)
  
  # Modify the IVy model to exclude these paths (find an efficient way)
  
  # Repeat with the new IVy model

  gf.cleanup()
