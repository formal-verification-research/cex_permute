// This provides a simulate package
package simulate;

// File handling
import java.io.File;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

// Array handling
import java.util.ArrayList;
import java.util.Arrays;

// PRISM Parser things
import parser.Values;
import parser.ast.Expression;
import parser.ast.ModulesFile;

// PRISM things
import prism.Prism;
import prism.PrismDevNullLog;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismPrintStreamLog;

// PRISM Simulator
import simulator.SimulatorEngine;

// This class takes care of everything we need the PRISM API for
public class BuildModel
{

  // By default, call BuildModel().run()
  public static void main(String[] args)
  {
    new BuildModel().run();
  }

  // Store the transitions in their own objects
  public class Transition {
    public int from; // transition from this state index
    public int to; // transition to this state index
    public double rate; // rate for this transition
    public int transitionIndex = 0; // if a transition index is assigned, it goes here
    public String transitionName = "t"; // human-readable name of this transition

    // Initialization function, nothing fancy
    public Transition(int f, int t, double r, int i, String n) {
      this.from = f;
      this.to = t;
      this.rate = r;
      this.transitionIndex = i;
      this.transitionName = n;
    }

    // Custom string output to give the most detail
    @Override
    public String toString() {
      return this.from + " " + this.to + " " + this.rate + " (" + this.transitionIndex + "_" + this.transitionName;
    }

    // Output the transition to go in the .tra output file
    // The format for .tra files is <from> <to> <rate>
    public String prism() {
      // subtract 1 from the indices so that the initial state can be 1,
      // not 0, but PRISM can read it in as state 0.
      return (this.from-1) + " " + (this.to-1) + " " + this.rate;
    }

  }

  // Store states in their own objects
  public class State {
    public int index; // State index
    public ArrayList<Integer> vars; // State variable values
    public double totalRate; // Total outgoing rate at this state
    public ArrayList<String> enabled; // Set of enabled outgoing transitions
    public ArrayList<Transition> outgoing; // Set of saved outgoing transitions

    // Initialize a state, nothing fancy here
    public State(int index, ArrayList<Integer> vars, double totalRate) {
      this.index = index;
      this.vars = vars;
      this.totalRate = totalRate;
      this.enabled = new ArrayList<String>();
      this.outgoing = new ArrayList<Transition>();
    }

    // State details for printing
    @Override
    public String toString() {
      String temp = index + " [";
      for (int i=0; i<vars.size(); i++) {
        if (i>0) temp += ",";
        temp += vars.get(i); 
      }
      temp += ("] (" + totalRate + ")");
      return temp;
    }

    // State details for .sta files
    // Format is <index>:(<state_var>,<state_var>...)
    public String prism() {
      String temp = (index-1) + ":(";
      for (int i=0; i<vars.size(); i++) {
        if (i>0) temp += ",";
        temp += vars.get(i); 
      }
      return temp + ")";
    }

    // Add to the total outgoing rate, and return the new total rate
    public double addRate(double p) {
      this.totalRate += p;
      return this.totalRate;
    }

    // Check if state variables are all equal (i.e. equivalent states)
    public boolean equals(State s) {
      // Different length clearly indicates a difference
      if (this.vars.size() != s.vars.size()) return false;
      // Check each value one-by-one
      for (int i=0; i<this.vars.size(); i++) {
        if (this.vars.get(i) != s.vars.get(i)) return false;
      }
      return true;
    }

    // Add an outgoing transition to the list
    public void addTransition(int to, double rate, int transitionIndex, String transitionName) {
      outgoing.add(new Transition(this.index, to, rate, transitionIndex, transitionName));
    }

    // Get the rate of transitions into the absorbing state
    // absRate = (total rate) - (sum of outgoing rates)
    public double getAbsorbingRate() {
      double absorbRate = this.totalRate;
      for (int i = 0; i < outgoing.size(); i++) {
        absorbRate -= outgoing.get(i).rate;
      }
      return absorbRate;
    }

    // Add an enabled transition to the list
    public void addEnabled(String transition_name) {
      enabled.add(transition_name);
    }

  }

  // Store a path in an object
  public class Path {
    
    public int firstState; // first state index
    public int lastState; // last state index
    public ArrayList<String> prefix; // list of commutable transitions
    public ArrayList<String> commutable; // list of commutable transitions

    // Initialization function, nothing fancy
    public Path(int firstState, int lastState) {
      this.firstState = firstState;
      this.lastState = lastState;
      this.prefix = new ArrayList<String>();
      this.commutable = new ArrayList<String>();
    }

    // Initialization function WITH PREFIX, nothing fancy
    public Path(int firstState, int lastState, ArrayList<String> prefix) {
      this.firstState = firstState;
      this.lastState = lastState;
      this.prefix = prefix;
      this.commutable = new ArrayList<String>();
    }

    // Find the intersection of states
    public void findCommutable(ArrayList<State> states) {
      // Initialize the arraylists for checking
      ArrayList<String> wasEnabled = states.get(this.firstState).enabled;
      ArrayList<String> isEnabled = new ArrayList<String>();
      // Loop through the states to check enabled
      for (int i = this.firstState + 1; i < this.lastState; i++) {
        wasEnabled = isEnabled;
        isEnabled = new ArrayList<String>();
        for (int j = 0; j < states.get(i).enabled.size(); j++) {
          // If the last enabled transitions contain the transition
          if (wasEnabled.contains(states.get(i).enabled.get(j))) {
            isEnabled.add(states.get(i).enabled.get(j));
          }
        }
      }
      this.commutable = isEnabled;
    }

    // Remove a commutable (because it doesn't work)
    public void removeCommutable(int c) {
      commutable.remove(c);
    }

    // Custom string output to give the most detail
    @Override
    public String toString() {
      return "Path object spanning state " + this.firstState + " through state " + this.lastState + " with commutable transitions " + commutable.toString();
    }

  }

  // Store model information in an object
  public class Model {
    
    public int stateCount;
    public int currentState;
    public int lastPathEnded;
    public ArrayList<State> states;
    
    public int n;

    public int pathCount;
    public ArrayList<Path> paths;

    public int absorbingIndex;

    // Initialization function, nothing fancy
    public Model() {
      this.stateCount = 0;
      this.currentState = -1;
      this.lastPathEnded = 0;
      this.n = 0;
      this.pathCount = 0;
      this.states = new ArrayList<State>();
      this.paths = new ArrayList<Path>();
      this.absorbingIndex = -1;
    }
    
    public void setN(int n) {
      this.n = n;
    }

    // Create a new state with a new index
    public void addState(ArrayList<Object> vars, double totalRate) {
      ArrayList<Integer> intVars = new ArrayList<Integer>();
      for (int i = 0; i < vars.size(); i++) {
        // Check if Object is an Integer or a String, handle appropriately
        if (vars.get(i) instanceof Integer) {
          intVars.add((Integer) vars.get(i));
        }
        else if (vars.get(i) instanceof String) {
          intVars.add(Integer.parseInt((String) vars.get(i)));
        }
      }
      this.states.add(new State(stateCount, intVars, totalRate));
      stateCount++;
      currentState++;
    }

    // Add to the total rate of the current state
    public void addRateToCurrentState(double rate) {
      states.get(currentState).addRate(rate);
    }
    
    // Add to the enabled transitions of the current state
    public void addEnabledToCurrentState(String transition) {
      states.get(currentState).addEnabled(transition);
    }

    // Add a one-step transition to the current state
    public void addSimpleTransition(int index, String name, double rate) {
      states.get(currentState).addTransition(currentState + 1, rate, index, name);
    }
    
    // Add a more complicated transition
    public void addTransition(int from, int to, int index, String name, double rate) {
      states.get(from).addTransition(to, rate, index, name);
    }

    // Make a seed path object
    public void addPath() {
      this.paths.add(new Path(lastPathEnded, currentState));
      lastPathEnded = currentState + 1;
      pathCount++;
    }

    // Make a seed path object WITH PREFIX
    public void addPath(ArrayList<String> prefix) {
      this.paths.add(new Path(lastPathEnded, currentState, prefix));
      lastPathEnded = currentState + 1;
      pathCount++;
    }

    // TODO: Might need to make a custom start/end path builder?
    

    // Make an absorbing state
    //  This MUST be the last state you add.
    public void addAbsorbingState() {
      int numVars = states.get(0).vars.size();
      ArrayList<Integer> absorbingVariables = new ArrayList<Integer>();
      // Set all absorbing variables to -1
      for (int i = 0; i < numVars; i++) {
        absorbingVariables.add(Integer.valueOf(-1));
      }
      this.absorbingIndex = stateCount;
      this.states.add(new State(absorbingIndex, absorbingVariables, 0.0));
      System.out.println("Absorbing Index: " + absorbingIndex);
      double absorbRate = 0.0;
      for (int i = 0; i < states.size() - 1; i++) {
        absorbRate = states.get(i).getAbsorbingRate();
        if (absorbRate > 0.0) {
          states.get(i).addTransition(absorbingIndex, states.get(i).getAbsorbingRate(), -1, "ABSORB");
        }
      }
    }

    public int countTransitions() {
      int transitionCount = 0;
      for (int i = 0; i < states.size() - 1; i++) {
        transitionCount += states.get(i).outgoing.size();
      }
      return transitionCount;
    }

    // Export model files
    public void exportFiles(SimulatorEngine sim) {
      try {
      // Count transitions
      int transitionCount = this.countTransitions();

      // Initialize transition string
      String traStr = "";
      traStr += ((states.size()) + " " + transitionCount + "\n");

      // Initialize state string
      String staStr = "";
      staStr += "(";

      // Initialize label string
      // TODO: Play with deadlock vs sink
      String labStr = "0=\"init\" 1=\"sink\"\n0: 0\n";
      labStr += (this.absorbingIndex + ": 1");
        
      // Initialize variable name string
      String varName = "";

      // Get variable names for first line of .sta file
      // TODO: This might actually be useful early on, to
      //  get the total variable count?
      int vari = 0;
      while (true) {
        varName = sim.getVariableName(vari);
        staStr += varName;
        if (sim.getVariableName(vari+1) == null) {
          staStr += ")\n";
          break;
        }
        staStr += ",";
        vari++;
      }

        // Get state and transition info by looping through states;
        //  state 0 is a filler state to make for easier math,
        //  thus, start state loop at index 1, not 0
        for (int i = 1; i < states.size(); i++) {
          // Loop through transitions at each state
          for (int j = 0; j < states.get(i).outgoing.size(); j++) {
            traStr += states.get(i).outgoing.get(j).prism();
            traStr += "\n";
          }
          staStr += states.get(i).prism();
          staStr += "\n";
        }

        // Write the state file to buildModel.sta
        BufferedWriter staWriter = new BufferedWriter(new FileWriter("buildModel.sta"));
        staWriter.write(staStr);
        staWriter.close();

        // Write the transition file to buildModel.tra
        BufferedWriter traWriter = new BufferedWriter(new FileWriter("buildModel.tra"));
        traWriter.write(traStr);
        traWriter.close();

        // Write the label file to buildModel.lab
        BufferedWriter labWriter = new BufferedWriter(new FileWriter("buildModel.lab"));
        labWriter.write(labStr);
        labWriter.close();

        // Alert user of successful termination
        System.out.println("Model built. PRISM API ended successfully.");
      }
      // Catch common errors and give user the info
      catch (FileNotFoundException e) {
        System.out.println("FileNotFoundException Error: " + e.getMessage());
        System.exit(1);
      } 
      catch (IOException e) {
        System.out.println("IOException Error: " + e.getMessage());
        System.exit(1);
      }
    }

  }

  // Build a path following a transition sequence after firing a prefix
  public boolean buildPath(Prism prism, ArrayList<String> transitions, ArrayList<String> prefix, Model model) {
  try {
    // Create a new simulation
    SimulatorEngine sim = prism.getSimulator();
    sim.createNewPath();
    sim.initialisePath(null);

    int index;
    
    // Walk along the prefix to get the new initial state
    for (int t=0; t < prefix.size(); t++) {

      // Reset the index
      index = 0;

      // Get information for the current transition
      for (int i=0; i < sim.getNumTransitions(); i++) {
        // Get transition strings from path and simulation
        String pathTransition = String.format("[%s]", prefix.get(t));
        String simTransition = sim.getTransitionActionString(i);
        // Update index if we found the right transition
        if (pathTransition.equalsIgnoreCase(simTransition)) {
            index = i;
            break;
        }
      }

      // Fire the prefix transition
      sim.manualTransition(index);

    }



    // Walk along the path and add state info to the model
    for (int t=0; t < transitions.size(); t++) {

      // Make the current state
      Object varVals[] = sim.getCurrentState().varValues;
      ArrayList<Object> stateVariables = new ArrayList<Object>();
      for (int i = 0; i < varVals.length; i++) {
        stateVariables.add(varVals[i]);
      }
      model.addState(stateVariables, 0.0);

      // Reset the index
      index = 0;

      // Get information for the current transition
      for (int i=0; i < sim.getNumTransitions(); i++) {
        // Get transition strings from path and simulation
        String pathTransition = String.format("[%s]", transitions.get(t));
        String simTransition = sim.getTransitionActionString(i);
        // Add current state info to model
        model.addRateToCurrentState(sim.getTransitionProbability(i));
        model.addEnabledToCurrentState(simTransition);
        // Update index if we found the right transition
        if (pathTransition.equalsIgnoreCase(simTransition)) {
            index = i;
        }
      }

      // Get the rate of the transition we fired
      double transition_probability = sim.getTransitionProbability(index);

      // If the transition probability is 0, alert the user
      if (transition_probability == 0.0f) {
        System.out.println(String.format("ZERO PROBABILITY FOR TRANSITION %s (%d)", sim.getTransitionActionString(index), index));
      }

      // Add the transition to the model
      model.addSimpleTransition(index, sim.getTransitionActionString(index), sim.getTransitionProbability(index));

      // Fire the transition
      sim.manualTransition(index);

    }

    // Make the target state (after firing the final transition)
    Object varVals[] = sim.getCurrentState().varValues;
    ArrayList<Object> stateVariables = new ArrayList<Object>();
    for (int i = 0; i < varVals.length; i++) {
      stateVariables.add(varVals[i]);
    }
    model.addState(stateVariables, 0.0);

    // Check if it's a target state and alert the user if not
    // TODO: Implement this

    // Tell the model we finished the seed path
    model.addPath(prefix);
    return true;
  }
  // Catch common errors and give user the info
  catch (PrismException e) {
    System.out.println("PrismException Error: " + e.getMessage());
    System.exit(1);
    return false;
  }
  }

  // Build parallel commuted paths for a path
  public void commute(Prism prism, Model model, Path path, ArrayList<String> transitions, int depth) {
  try {
    
    // Maximum recursion depth
    if (depth == 2) {
      return;
    }

    // Find the commutable transitions along the path
    path.findCommutable(model.states);

    // Set up array to mark transitions for removal
    boolean toRemove[] = new boolean[path.commutable.size()];
    Arrays.fill(toRemove, false);

    // Check the full-lenth parallel path before firing commutable transitions
    // That is, set the commutable transition as a path prefix
    //  and build paths following the seed transition sequence.
    for (int c = 0; c < path.commutable.size(); c++) {
      ArrayList<String> nextPrefix = new ArrayList<String>();
      nextPrefix.addAll(path.prefix);
      nextPrefix.add(path.commutable.get(c));
      // TODO: Also check if we reached a target state within the buildPath function
      if (buildPath(prism, transitions, nextPrefix, model) == false) {
        toRemove[c] = true;
        continue;
      }
    }
    
    // Remove the necessary transitions from commutable
    for (int i = toRemove.length - 1; i >= 0 ; i--) {
      if (toRemove[i]) {
        path.removeCommutable(i);
      }
    }

    // Count how many paths we go back when we look at indices
    int addedPaths = path.commutable.size();
    int goBackPaths = addedPaths;

    // Fire each commutable transition from each state along the path
    //  and check that it matches the full-length path

    // Do this for every commutable transition
    for (int t_alpha = 0; t_alpha < path.commutable.size(); t_alpha++) {

      // Create a new simulation from the initial state
      SimulatorEngine sim = prism.getSimulator();
      sim.createNewPath();
      sim.initialisePath(null);

      int index;

      // This stores where we are adjacent to in the SEED PATH
      int seedStateIndex = 0;
      int simStateIndex = 0;

      // TODO: There may be more math to deal with prefixes.
      //  Look at this when I have the brain power

      // Walk along the prefix to get the new initial state
      for (int t=0; t < path.prefix.size(); t++) {

        // Reset the index
        index = 0;

        // Get information for the current transition
        for (int i=0; i < sim.getNumTransitions(); i++) {
          // Get transition strings from path and simulation
          String pathTransition = String.format("[%s]", path.prefix.get(t));
          String simTransition = sim.getTransitionActionString(i);
          // Update index if we found the right transition
          if (pathTransition.equalsIgnoreCase(simTransition)) {
              index = i;
              break;
          }
        }

        // Fire the prefix transition
        sim.manualTransition(index);
        simStateIndex++;

      }

      // Walk along the path, firing and adding the commutable transition
      //  then backtracking to the previous state
      for (int t=0; t < transitions.size(); t++) {

        // Reset the index
        index = 0;
        String finalTransition = "";

        // Find and fire the commuted transition
        for (int i=0; i < sim.getNumTransitions(); i++) {
          // Get transition strings from path and simulation
          String pathTransition = String.format("[%s]", path.commutable.get(t_alpha));
          String simTransition = sim.getTransitionActionString(i);
          // Update index if we found the right transition
          if (pathTransition.equalsIgnoreCase(simTransition)) {
            index = i;
            finalTransition = simTransition;
            break;
          }
        }
        // Fire the transition without incrementing simStateIndex, 
        //  since we want to store the index of the state we return to
        sim.manualTransition(index);

        // Check that the commuted transition is independent. If yes, save the transition
        Object varVals[] = sim.getCurrentState().varValues;
        ArrayList<Integer> stateVariables = new ArrayList<Integer>();
        for (int i = 0; i < varVals.length; i++) {
          if (varVals[i] instanceof Integer) {
            stateVariables.add((Integer) varVals[i]);
          }
          else if (varVals[i] instanceof String) {
            stateVariables.add(Integer.parseInt((String) varVals[i]));
          }
          // stateVariables.add(varVals[i]);
        }

        State tempState = new State(-5, stateVariables, 0);
        
        // Check if the new state is equal to state in pre-calculated path

        // Index of the equivalent state:
        // (adjacent index) + ( (path id) * (n) )
        // (adjacent index) + ( ( (path count) - (go back paths) ) * (n) )
        // Math from back of envelope from Molina

        int equivalentIndex = seedStateIndex + ( (model.pathCount - goBackPaths) * model.n );

        System.out.println("EQUIVALENT INDEX IS: " + equivalentIndex);
        
        // Check is state matches what it should be
        if (tempState.equals(model.states.get(equivalentIndex))) {
          System.out.println("STATES EQUIVALENT: " + tempState + " and " + model.states.get(equivalentIndex));
          // Add transition to the relevant state
          int from = seedStateIndex + path.firstState;
          int to = equivalentIndex;
          // index is still index
          String name = sim.getTransitionActionString(index);
          double rate = sim.getTransitionProbability(index);
          model.addTransition(from, to, index, name, rate);
        }
        else {
          System.out.println("STATES NOT EQUIVALENT: " + tempState + " and " + model.states.get(equivalentIndex));
        }

        // Backtrack
        sim.backtrackTo(simStateIndex);

        // Find the original path transition
        for (int i=0; i < sim.getNumTransitions(); i++) {
          // Get transition strings from path and simulation
          String pathTransition = String.format("[%s]", transitions.get(t));
          String simTransition = sim.getTransitionActionString(i);
          // Update index if we found the right transition
          if (pathTransition.equalsIgnoreCase(simTransition)) {
              index = i;
          }
        }

        // Fire the transition
        sim.manualTransition(index);
        simStateIndex++;
        seedStateIndex++;

      }
      
      // We're moving on to the next path
      goBackPaths--;

    }
  
    for (int i = addedPaths; i > 0; i--) {
      commute(prism, model, model.paths.get(i), transitions, depth + 1);
    }
  }
  // Catch common errors and give user the info
  catch (PrismException e) {
    System.out.println("PrismException Error: " + e.getMessage());
    System.exit(1);
  }
  }

  // Main run function - runs every call
  public void run()
  {
    try {
      
      // Welcome message
      System.out.println("Building PRISM Model based on seed path.");
      
      // Keep an index available for temporary use
      int index;
      
      // give PRISM output a log file
      PrismLog mainLog = new PrismDevNullLog();

      // initialise (with an s) PRISM engine
      Prism prism = new Prism(mainLog);
      prism.initialise();

      // Parse the prism model
      // For now, model.sm is the hard-coded model file name
      ModulesFile modulesFile = prism.parseModelFile(new File("model.sm"));

      // Load the prism model for checking
      prism.loadPRISMModel(modulesFile);

      // if the model has unknown constant values, deal with that here.
      // for now, we assume NO UNDEFINED CONSTANTS

      // Load the model into the simulator engine
      prism.loadModelIntoSimulator();

      // Initialize the model
      Model model = new Model();

      // Read in the first line of the trace as a string
      // For now, the trace must go in forprism.trace (handled in python script)
			FileReader fr = new FileReader("forprism.trace");
			BufferedReader br = new BufferedReader(fr);

      // Read in the seed path line
      String x_transitions = br.readLine();
      String[] strarr_transitions = x_transitions.split("\\s+");
      ArrayList<String> transitions = new ArrayList<String>();
      for (int i = 0; i < strarr_transitions.length; i++) {
        transitions.add(strarr_transitions[i]);
      }
      // transitions = Arrays.asList(x_transitions.split("\\s+"));
      
      // Let n_seed be the length of the seed path (in STATES, not transitions)
      int n_seed = transitions.size() + 1;
      System.out.println("n_seed (states in seed path) = " + n_seed);
      model.setN(n_seed);

      // Build the seed path, which recursively does everything
      ArrayList<String> prefix = new ArrayList<String>();
      buildPath(prism, transitions, prefix, model);
      commute(prism, model, model.paths.get(0), transitions, 0); 

      // Configure the absorbing state
      model.addAbsorbingState();

      // Print to files
      SimulatorEngine sim = prism.getSimulator();
      model.exportFiles(sim);

      // close PRISM
      prism.closeDown();

    } 
    // Catch common errors and give user the info
    catch (PrismException e) {
			System.out.println("PrismException Error: " + e.getMessage());
			System.exit(1);
		}
    catch (IOException e) {
      System.out.println("IOException Error: " + e.getMessage());
			System.exit(1);
    }
  }
}