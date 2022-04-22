import os

def get_intersection(trace):

    trace_a = trace.replace("]","").replace("[","")
    trace = ""

    last_enabled = []
    curr_enabled = []

    # Clean up after the binary file
    # TODO make a better binary file
    for line in trace_a.split("\n"):
        if "bin" not in line and "length" not in line:
            trace = trace + line + "\n"
    print("TRACE BELOW:")
    print(80*"*")
    print(trace)
    print(80*"*")

    # if "TARGET" not in trace:
    #     print("ERROR: DOES NOT APPEAR TO REACH TARGET.")
    #     print("ERROR THROWN BY prism_api.py")

    curr_enabled = trace.split("\n")[0].split(" ")
    # print("curr enabled")
    # print(curr_enabled)

    for line in trace.split("\n"):
        if ">>" in line:
            break
        if "pathProbability " in line:
            print("path probability found: " + line.split("pathProbability ")[1])
            break
        last_enabled = curr_enabled
        curr_enabled = []
        # print("curr_enabled = " + str(curr_enabled))
        # print("last_enabled = " + str(last_enabled))
        for transition in line.split(" "):
            if transition in last_enabled:
                curr_enabled.append(transition)
    
    print(str(curr_enabled))
    return curr_enabled


def get_available(trace, index):

    trace_a = trace.replace("]","").replace("[","")
    trace = ""

    # Clean up after the binary file
    # TODO make a better binary file
    for line in trace_a.split("\n"):
        if "bin" not in line and "length" not in line and ">>" not in line and "pathProbability" not in line:
            trace = trace + line + "\n"
    # print("ALL AVAILABLE TRANSITIONS BELOW:")
    # print(80*"*")
    # print(trace)
    # print(80*"*")

    available = trace.split("\n")[index].split(" ")

    # for line in trace.split("\n"):
    #     if ">>" in line:
    #         break
    #     if "pathProbability " in line:
    #         print("path probability found: " + line.split("pathProbability ")[1])
    #         break
    #     last_enabled = curr_enabled
    #     curr_enabled = []
    #     # print("curr_enabled = " + str(curr_enabled))
    #     # print("last_enabled = " + str(last_enabled))
    #     for transition in line.split(" "):
    #         if transition in last_enabled:
    #             curr_enabled.append(transition)
    
    # print(str(curr_enabled))
    return available