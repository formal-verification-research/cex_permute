

def get_intersection(trace):

    last_enabled = []
    curr_enabled = []

    if "TARGET" not in trace:
        print("ERROR: DOES NOT APPEAR TO REACH TARGET.")
        print("ERROR THROWN BY prism_api.py")

    curr_enabled = trace.split("\n")[0].split(" ")

    for line in trace.split("\n"):
        if "TARGET" in line:
            break
        if "totalProbability " in line:
            print("probability found: " + line.split("totalProbability ")[1])
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

