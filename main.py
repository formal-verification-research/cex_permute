
# DEPENDENCIES: 

# IVy with PDR enabled:
# http://microsoft.github.io/ivy/install

# PRISM Installation
# https://github.com/prismmodelchecker/prism

# PRISM API interface by Chris Winstead: 
# https://github.com/fluentverification/usu_importance_sampling


# This version is hard-coded for the 8-reaction (yeast polarization) model


# Import functions from local files
import ivy
import prism_api
import commute
import pathProb
import utils
import branch

# import functions from python libraries
import os
import sys
import tempfile


# Main procedure
if __name__ == "__main__":

  depth = 6

  # Clean up the folders we're working in
  utils.cleanup()

  # Print a welcome message to stdout and stderr
  utils.printall(80*"*" + "\nWelcome to the counterexample permutation explorer.\n" + 80*"*")

  # Get the names of files
  # ivy_file = utils.get_ivy_file()
  # prism_file = utils.get_prism_file()
  
  # Run ivy_check to get the seed counterexample
  # utils.printall("Running ivy_check on the model...")
  # ivy_path = ivy.check(ivy_file)
  
  # Get the seed path from a file instead of running ivy_check,
  #   since we already have the path.
  # with open("model.trace", 'r') as ivy_trace:
  with open("mo.trace", 'r') as ivy_trace:
    ivy_path = ivy_trace.read().replace(" ", "\t")
  # uncomment the next 2 lines to use the shortest path instead
  # with open("shortest.trace", 'r') as ivy_trace:
  #   ivy_path = ivy_trace.read().replace(" ", "\t")

  # # Get all enabled transitions at each state along the seed path
  # utils.printall("Getting enabled transitions")
  # api_result = prism_api.getEnabledTransitions(ivy_path)
  # utils.printall("Enabled transitions are" + str(api_result))

  # # Find the intersection of all enabled transition sets
  # utils.printall("Getting intersection of enabled transitions")
  # intersection = prism_api.get_intersection(api_result)
  # utils.printall("Intersection is " + str(intersection))

  # Print the file to send to the PRISM API. The format is:
  #   BUILD_MODEL
  #   commutable transitions, tab-separated
  #   seed path
  utils.printall("Printing file to send to prism API")
  with open("forprism.trace", "w") as p:
    # Print BUILD_MODEL keyword (NOT OPTIONAL)
    # p.write("BUILD_MODEL\n")
    # Print the tab-separated commutable transitions
    # for i in range(0,len(intersection)):
    #   if i>0:
    #     p.write("\t")
    #   p.write(intersection[i])
    # p.write("\n")
    # Print the tab-separated seed path
    p.write(ivy_path)

  # Send the file to the PRISM API to build the state-transition matrix
  utils.printall("Sending model to prism API")
  prism_api.buildmodel()
  
  # Call PRISM from command line to model check the constructed model
  report_name = "sm_mo" + str(depth) + ".txt"
  utils.printall("Using PRISM to model check. See " + report_name)
  os.system("prism -importmodel buildModel.tra,sta,lab -exportmodel out.tra,sta,lab -ctmc pro.csl > " + report_name)

  # Give exit message
  utils.printall(80*"=")
  utils.printall("Exiting without error.")
  utils.printall(80*"=")
  
  # User can check the probability in final_prism_report.txt
