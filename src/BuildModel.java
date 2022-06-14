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
    public ArrayList<Object> vars; // State variable values
    public double totalRate; // Total outgoing rate at this state
    public ArrayList<String> enabled; // Set of enabled outgoing transitions
    public ArrayList<Transition> outgoing; // Set of saved outgoing transitions

    // Initialize a state, nothing fancy here
    public State(int index, ArrayList<Object> vars, double totalRate) {
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
      for (int i=0; i<vars.length; i++) {
        if (i>0) temp += ",";
        temp += vars[i]; 
      }
      temp += ("] (" + totalRate + ")");
      return temp;
    }

    // State details for .sta files
    // Format is <index>:(<state_var>,<state_var>...)
    public String prism() {
      String temp = (index-1) + ":(";
      for (int i=0; i<vars.length; i++) {
        if (i>0) temp += ",";
        temp += vars[i]; 
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
      if (this.vars.length != s.vars.length) return false;
      // Check each value one-by-one
      for (int i=0; i<this.vars.length; i++) {
        if (this.vars[i] != s.vars[i]) return false;
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

    // Initialization function, nothing fancy
    public Model() {
      this.stateCount = 0;
      this.currentState = -1;
      this.lastPathEnded = 0;
      this.n = 0;
      this.states = new ArrayList<State>();
      this.paths = new ArrayList<Path>();
    }
    
    public void setN(int n) {
      this.n = n;
    }

    // Create a new state with a new index
    public void addState(ArrayList<Object> vars, double totalRate) {
      this.states.add(new State(stateCount, vars, totalRate));
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

    // Make a seed path object
    public void addPath() {
      this.paths.add(new Path(lastPathEnded, currentState));
      lastPathEnded = currentState + 1;
    }

    // Make a seed path object WITH PREFIX
    public void addPath(ArrayList<String> prefix) {
      this.paths.add(new Path(lastPathEnded, currentState, prefix));
      lastPathEnded = currentState + 1;
    }


    // // Custom string output to give the most detail
    // @Override
    // public String toString() {
    //   return "Path object spanning state " + this.firstState + " through state " + this.lastState + " with commutable transitions " + commutable.toString();
    // }

  }

  // Build a path following a transition sequence after firing a prefix
  public boolean buildPath(ArrayList<String> transitions, ArrayList<String> prefix, Model model, int depth) {
  
    // Maximum recursion depth
    if (depth == 1) {
      return false;
    }

    // Create a new simulation
    SimulatorEngine sim = prism.getSimulator();
    sim.createNewPath();
    sim.initialisePath(null);
    
    boolean foundIt = false;

    // Walk along the prefix to get the new initial state
    for (int t=0; t < prefix.size(); t++) {

      // Reset the index
      index = 0;
      boolean foundIt = false;

      // Get information for the current transition
      for (int i=0; i < sim.getNumTransitions(); i++) {
        // Get transition strings from path and simulation
        String pathTransition = String.format("[%s]", prefix.get(t));
        String simTransition = sim.getTransitionActionString(i);
        // Update index if we found the right transition
        if (pathTransition.equalsIgnoreCase(simTransition)) {
            index = i;
            foundIt = true;
            break;
        }
      }
      
      if (!foundIt) {
        return false;
      }

      // Fire the prefix transition
      sim.manualTransition(index);

    }

    // Walk along the path and add state info to the model
    for (int t=0; t < transitions.size(); t++) {

      // Make the current state
      model.addState(sim.getCurrentState().varValues, 0.0);

      // Reset the index
      index = 0;

      // Get information for the current transition
      for (int i=0; i < sim.getNumTransitions(); i++) {
        // Get transition strings from path and simulation
        String pathTransition = String.format("[%s]", transitions.get(t));
        String simTransition = sim.getTransitionActionString(i);
        // Add current state info to model
        model.addRateToCurrentState(simTransition);
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

      // Fire the transition
      sim.manualTransition(index);

    }

    // Make the target state (after firing the final transition)
    model.addState(sim.getCurrentState().varValues, 0.0);

    // Check if it's a target state and alert the user if not
    // TODO: Implement this

    // Tell the model we finished the seed path
    model.addPath(prefix);

    return true;
  }

  public void commute(Model model, Path path, ArrayList<String> transitions, int depth) {

    // Find the commutable transitions along the path
    path.findCommutable();

    // Set up array to mark transitions for removal
    boolean toRemove[] = new boolean[path.commutable.size()];
    Arrays.fill(toRemove, false);

    // Check the full-lenth parallel path before firing commutable transitions
    // That is, set the commutable transition as a path prefix
    //  and build paths following the seed transition sequence.
    for (int c = c < path.commutable.size(); c++) {
      ArrayList<String> nextPrefix = new ArrayList<String>();
      nextPrefix.addAll(path.prefix);
      nextPrefix.add(path.commutable.get(c));
      if (!buildPath(transitions, nextPrefix, model, depth+1)) {
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

    // Fire each commutable transition from each state along the path
    //  and check that it matches the full-length path

    // Do this for every commutable transition
    for (int t_alpha = 0; t_alpha < path.commutable.size(); t_alpha++) {

      // Create a new simulation from the initial state
      SimulatorEngine sim = prism.getSimulator();
      sim.createNewPath();
      sim.initialisePath(null);

      

      int currentStateIndex = path.firstState;

      // Walk along the prefix to get the new initial state
      for (int t=0; t < path.prefix.size(); t++) {

        // Reset the index
        index = 0;
        boolean foundIt = false;

        // Get information for the current transition
        for (int i=0; i < sim.getNumTransitions(); i++) {
          // Get transition strings from path and simulation
          String pathTransition = String.format("[%s]", path.prefix.get(t));
          String simTransition = sim.getTransitionActionString(i);
          // Update index if we found the right transition
          if (pathTransition.equalsIgnoreCase(simTransition)) {
              index = i;
              foundIt = true;
              break;
          }
        }
        
        if (!foundIt) {
          return false;
        }

        // Fire the prefix transition
        sim.manualTransition(index);

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
        sim.manualTransition(index);

        // Check that the commuted transition is independent. If yes, save the transition
        State tempState = new State(-5, ArrayList<Object> vars, 0);
        
        // Check if the new state is equal to state (i + id*n)
        // TODO: This math is pending
        // if (tempState.equals(model.states.get(currentStateIndex - path.firstState)));



        // Backtrack
        sim.backtrackTo(currentStateIndex);



        // Fire the path transition
        for (int i=0; i < sim.getNumTransitions(); i++) {
          // Get transition strings from path and simulation
          String pathTransition = String.format("[%s]", transitions.get(t));
          String simTransition = sim.getTransitionActionString(i);

          // Update index if we found the right transition
          if (pathTransition.equalsIgnoreCase(simTransition)) {
              index = i;
          }
        }

        currentStateIndex++;


        



        // Get information for the current transition
        for (int i=0; i < sim.getNumTransitions(); i++) {
          // Get transition strings from path and simulation
          String pathTransition = String.format("[%s]", transitions.get(t));
          String simTransition = sim.getTransitionActionString(i);
          // Add current state info to model
          model.addRateToCurrentState(simTransition);
          model.addEnabledToCurrentState(simTransition);
          // Update index if we found the right transition
          if (pathTransition.equalsIgnoreCase(simTransition)) {
              index = i;
          }
        }

        // Make the current state
        model.addState(sim.getCurrentState().varValues, 0.0);



        // Get the rate of the transition we fired
        double transition_probability = sim.getTransitionProbability(index);

        // If the transition probability is 0, alert the user
        if (transition_probability == 0.0f) {
          System.out.println(String.format("ZERO PROBABILITY FOR TRANSITION %s (%d)", sim.getTransitionActionString(index), index));
        }

        // Fire the transition
        sim.manualTransition(index);

    }



    }


























    
    boolean foundIt = false;

    // Walk along the path and add state info to the model
    

    // Make the target state (after firing the final transition)
    model.addState(sim.getCurrentState().varValues, 0.0);


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

      // // Create a new simulation
      // SimulatorEngine sim = prism.getSimulator();
      // sim.createNewPath();
      // sim.initialisePath(null);

      /* ******************************************************************** */
      // Construct the seed path
      /* ******************************************************************** */
		
      // Read in the first line of the trace as a string
      // For now, the trace must go in forprism.trace (handled in python script)
			FileReader fr = new FileReader("forprism.trace");
			BufferedReader br = new BufferedReader(fr);

      // Read in the seed path line
      String x_transitions = br.readLine();
      ArrayList<String> transitions = new ArrayList<String>();
      transitions = Arrays.asList(x_transitions.split("\\s+"));
      
      // Let n_seed be the length of the seed path (in STATES, not transitions)
      int n_seed = transitions.size() + 1;
      System.out.println("n_seed (states in seed path) = " + n_seed);
      model.setN(n_seed);

      // Build the seed path
      ArrayList<String> prefix = new ArrayList<String>();
      buildPath(transitions, prefix, model, 0);
















      // // Walk along the seed path to get state info

      // for (int t=0; t < path.size(); t++) {

      //   // Make the current state
      //   model.addState(sim.getCurrentState().varValues, 0.0);

      //   // Reset the index
      //   index = 0;

      //   // Get information for the current transition
      //   for (int i=0; i < sim.getNumTransitions(); i++) {
      //     // Get transition strings from path and simulation
      //     String pathTransition = String.format("[%s]", path.get(t));
      //     String simTransition = sim.getTransitionActionString(i);
      //     // Add current state info to model
      //     model.addRateToCurrentState(simTransition);
      //     model.addEnabledToCurrentState(simTransition);
      //     // Update index if we found the right transition
      //     if (pathTransition.equalsIgnoreCase(simTransition)) {
      //         index = i;
      //     }
      //   }

      //   // Get the rate of the transition we fired
      //   double transition_probability = sim.getTransitionProbability(index);

      //   // If the transition probability is 0, alert the user
      //   if (transition_probability == 0.0f) {
      //     System.out.println(String.format("ZERO PROBABILITY FOR TRANSITION %s (%d)", sim.getTransitionActionString(index), index));
      //   }

      //   // Fire the transition
      //   sim.manualTransition(index);

      // }

      // // Make the target state (after firing the final transition)
      // model.addState(sim.getCurrentState().varValues, 0.0);

      // // Tell the model we finished the seed path
      // model.addSeedPath();

      /* ******************************************************************** */
      // Fire each commutable transition from each state
      /* ******************************************************************** */







      // close PRISM
      prism.closeDown();


    } 
    // Catch common errors and give user the info
    catch (FileNotFoundException e) {
			System.out.println("FileNotFoundException Error: " + e.getMessage());
			System.exit(1);
		} 
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