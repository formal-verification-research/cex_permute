# Get the names of files we need to interact with.
# May be more sophisticated soon.

import subprocess

def get_ivy_file():
  return "model.ivy"

def get_prism_file():
  return "model.sm"

def cleanup():
  subprocess.run('rm -rf aigerfiles logfiles')