# Get the names of files we need to interact with.
# May be more sophisticated soon.

import subprocess
import os

def get_ivy_file():
  return "model.ivy"

def get_prism_file():
  return "model.sm"

def cleanup():
  subprocess.run(['rm','-rf','aigerfiles','logfiles'])
  try:
    os.system("make")
  except:
    print("FAILURE FAILURE FAILURE -- CLEANUP COULD NOT MAKE PRISM")