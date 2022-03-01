

def get_intersection(trace):

    trace = trace.replace("]","").replace("[","")

    last_enabled = []
    curr_enabled = []

    # Clean up after the binary file
    # TODO make a better binary file
    trace_a = trace.split("\n")
    for line in trace_a:
        if "bin/run" in line or "length" in line:
            del line
    trace = '\n'.join(trace_a)

    # if "TARGET" not in trace:
    #     print("ERROR: DOES NOT APPEAR TO REACH TARGET.")
    #     print("ERROR THROWN BY prism_api.py")

    curr_enabled = trace.split("\n")[0].split(" ")
    print("curr enabled")
    print(curr_enabled)

    for line in trace.split("\n"):
        if "TARGET" in line:
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

