# Printing and file handling utilities

import subprocess
import os
import sys

# Get the ivy model file name
def get_ivy_file():
  return "model.ivy"

# Get the prism model file name
def get_prism_file():
  return "model.sm"

# Clean up the working directory and make prism
def cleanup():
  subprocess.run(['rm','-rf','aigerfiles','logfiles'])
  try:
    os.system("make")
  except:
    print("FAILURE in utils.py (function cleanup) -- CLEANUP COULD NOT MAKE PRISM")

# Print to standard error instead
def printerr(*a):
    print(*a, file = sys.stderr)

# Print to stdout and stderr
def printall(*a):
    print(*a, file = sys.stderr)
    print(*a)