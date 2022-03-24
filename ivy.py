import subprocess

# CALL_HINT = "call ext:"
CALL_HINT = "call ext:spec."

def check(ivyfile):
  print("ivy_check trace=true " + str(ivyfile) + " > " + str(ivyfile).split(".")[0] + ".log")
  subprocess.run("ivy_check trace=true " + str(ivyfile) + " > " + str(ivyfile).split(".")[0] + ".log")
  subprocess.run("rm -rf aigerfiles logfiles ivy_mc.log")
  return str(ivyfile).split(".")[0] + ".log"
  # with open(str(ivyfile).split(".")[0] + ".log") as log:
  #   return log.read()

def get_path(logf):
  with open(logf) as log:
    trace = []
    for line in log:
      if CALL_HINT in line:
        action = line.split(CALL_HINT)[1].rstrip("\n")
        trace.append(action)
    final_trace = ""
    for item in trace:
      final_trace = final_trace + (item + "\t")
    return final_trace

def new_initial_state_6react():
  