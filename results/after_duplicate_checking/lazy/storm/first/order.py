

currentSource = 0
lines = []

with open("buildModel.tra", "r") as bm:
    with open("ordered.tra", "w") as ord:
        for line in bm:
            if "ctmc" in line:
                ord.write(line)
                continue

