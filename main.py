
# DEPENDENCIES: 

# IVy with PDR enabled:
# http://microsoft.github.io/ivy/install

# PRISM Installation
# https://github.com/prismmodelchecker/prism

# PRISM API interface by Chris Winstead: 
# https://github.com/fluentverification/usu_importance_sampling


# This version is hard-coded for the 8-reaction (yeast polarization) model


# Import functions from local files
# import getfiles as gf
import ivy
import prism_api
import commute
import pathProb
import os
import sys
import tempfile
import utils
import branch


def buildmodel():
  # copy in the dummy file, for testing only.
  # with open("dummy_build_model.txt") as t:
  #   with open("forprism.trace", "w") as p:
  #     p.write(t.read())
  try:
    os.system("make test")
  except:
    print("os.system Error in buildModel()")
    return



# Main procedure
if __name__ == "__main__":

  # Clean up the folders we're working in
  utils.cleanup()

  # Test the "build model" function
  # buildmodel()
  # utils.printall("BUILT MODEL.")

  #TODO: GENERATE LABEL FILE IN JAVA
  # should match dummy.lab, with 40 replaced with 2*n
  # os.system("prism -importmodel model.tra,sta,lab -exportmodel out.tra,sta,lab -ctmc pro.csl > final_prism_report.txt")
  # utils.printall("FINISHED PRISM")

  # model check
  # os.system("prism -importtrans model.tra -ctmc")
  # quit()

  # Set up necessary folders
  if not os.path.exists("results"):
    os.system("mkdir results")

  # Set up temp files for 
  # temp_result = tempfile.NamedTemporaryFile(mode="w+")

  utils.printall(80*"*","\nWelcome to the counterexample permutation explorer.\n",80*"*")

  # Set ourselves up to start gathering probability
  pathP = pathProb.pathProb()

  # Get the names of files
  ivy_file = utils.get_ivy_file()
  prism_file = utils.get_prism_file()
  
  # Run ivy_check to get the seed counterexample
  # ivyresult = ivy.check(ivy_file, temp_result)
  
  # get the seed path
  utils.printall("Running ivy_check on the model...")
  with open("model.trace", 'r') as ivy_trace:
    ivy_path = ivy_trace.read().replace(" ", "\t")
  
  # uncomment to use the shortest path instead
  # with open("shortest.trace", 'r') as ivy_trace:
  #   ivy_path = ivy_trace.read().replace(" ", "\t")

  # Use existing path since IVy Check takes FOREVER
  # ivy_path = ivy.check(ivy_file)

  # get the enabled transitions
  utils.printall("Getting enabled transitions")
  api_result = prism_api.getEnabledTransitions(ivy_path)
  utils.printall("Enabled transitions are" + str(api_result))

  # find the intersection of the transitions
  utils.printall("Getting intersection of enabled transitions")
  intersection = prism_api.get_intersection(api_result)
  utils.printall("Intersection is " + str(intersection))



  # print the output file
  utils.printall("Printing file to send to prism API")
  with open("forprism.trace", "w") as p:
    p.write("BUILD_MODEL\n")
    for i in range(0,len(intersection)):
      if i>0:
        p.write("\t")
      p.write(intersection[i])
    p.write("\n")
    p.write(ivy_path)

  # send the model to the api
  utils.printall("Sending model to prism API")
  buildmodel()
  

  # model check it
  utils.printall("Using PRISM to model check. See final_prism_report.txt")
  os.system("prism -importmodel model.tra,sta,lab -exportmodel out.tra,sta,lab -ctmc pro.csl > final_prism_report.txt")

  # Quit here for now
  # quit()


  # utils.printall(80*"=")
  # utils.printall("  Final Probability:", '%.10E' % pathP.prob)
  utils.printall(80*"=")
  utils.printall("Exiting without error.")
  utils.printall(80*"=")
  # printerr(80*"=")
  # printerr("  Final Probability:", '%.10E' % pathP.prob)
  # printerr(80*"=")
  # printerr("Exiting without error.")
  # printerr(80*"=")

  # Maybe joost-pieter's thing about storing a model instead of states
  
  # Modify the IVy model to exclude these paths (find an efficient way)
  
  # Repeat with the new IVy model

  # utils.cleanup()
