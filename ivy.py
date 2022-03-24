import subprocess
import re

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

def new_initial_state(i_state, old, new):
  # just get the state in the parentheses
  clean_i_state = i_state.split("(")[1]
  clean_i_state = clean_i_state.split(")")[0]
  i = clean_i_state.split(",")
  i_ivy = "after init {\n"
  i_ivy = i_ivy + "  s1 := " + i[0] + ";\n"
  i_ivy = i_ivy + "  s2 := " + i[1] + ";\n"
  i_ivy = i_ivy + "  s3 := " + i[2] + ";\n"
  i_ivy = i_ivy + "  s4 := " + i[3] + ";\n"
  i_ivy = i_ivy + "  s5 := " + i[4] + ";\n"
  i_ivy = i_ivy + "  s6 := " + i[5] + ";\n"
  i_ivy = i_ivy + "} #init\n\n"

  old_str = old.read()
  new_str = re.sub(r'after init\s+\{[^}]*\}\s+#init', i_ivy, old_str)

  print("*")
  print("NEW INITIALIZATION IN IVY")
  print(new_str)
  print("END NEW INITIALIZATION IN IVY")
  print("*")

  new.write(new_str)