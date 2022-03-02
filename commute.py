
# Commute the discovered transitions (intersection) throughout the trace.
def commute(trace, intersection):
  with open("commuted.trace", 'w') as commuted:
    # Get individual moves
    t = trace.replace("\t\t", "\t").rstrip().lstrip().split("\t")
    # Remove any empty spots in t
    while "" in t:
      t.remove("")
    # Loop through every discovered transition in the intersection
    for i in intersection:
      # Loop through the whole array
      for a in range(0,len(t)):
        # Get the commuted transition's "prefix"
        for b in range(0,a):
          commuted.write(t[b] + "\t")
        # Write the commuted transition
        commuted.write(i + "\t")
        # Get the commuted transition's "suffix"
        for c in range(a,len(t)):
          commuted.write(t[c] + "\t")
        # Reset for next trace
        commuted.write("\n")