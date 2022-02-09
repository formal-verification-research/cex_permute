
# DEPENDENCIES: 

# IVy with PDR enabled:
# http://microsoft.github.io/ivy/install

# PRISM API interface by Chris Winstead: 
# https://github.com/fluentverification/usu_importance_sampling


# Import functions from local files
import getfiles as gf
import ivy


# Main procedure
if __name__ == "__main__":
  
  print(80*"*")
  print("Welcome to the counterexample permutation explorer.")
  print(80*"*")

  # Get the names of files
  ivyfile = gf.get_ivyfile()
  prismfile = gf.get_prismfile()
  
  # Run ivy_check to get the seed counterexample
  print("Running ivy_check on your model...")
  ivyresult = ivy.check(ivyfile)

  # Extract the transition path from the counterexample
  print("Finding the counterexample transition path...")
  ivypath = ivy.get_path(ivyresult)

  # Save the path to a ivyfile.trace
  with open(str(ivyfile).split(".")[0] + ".trace", 'w') as trace:
    trace.write(ivypath)

  # TODO: There may be a way to remove transitions from a path.
  # That step should go here when it is developed.

  # Have the PRISM API walk along the path, reporting enabled transitions
  # Find the intersection of all enabled transitions
  # Build paths with the enabled transitions commuted
  # Test the commuted paths in PRISM (for dev stage)
  # Modify the IVy model to exclude these paths (find an efficient way)
  # Repeat with the new IVy model


"""
TODO

_   Install and check the PRISM API on Archibald
_   Find out how to remotely log into a home computer
_   Find out how to get a list of enabled transitions for each state
_   

"""