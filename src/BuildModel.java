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

  // Maximum recursion depth
  public static final int MAX_DEPTH = 0;

  // turn off printing to save time
  public static final boolean DO_PRINT = false;

  // static model name
  public static final String MODEL_NAME = "model.sm";
  // public static final String TRACE_LIST_NAME = "forprism.trace";
  // public static final String TRACE_LIST_NAME = "paths/manual/lazy.txt";
  // public static final String TRACE_LIST_NAME = "paths/ragtimer/a_20.txt";
  public static final String TRACE_LIST_NAME = "paths/other/shortest.txt";

  // By default, call BuildModel().run()
  public static void main(String[] args)
  {
    new BuildModel().run();
  }

  public int[] getIntVarVals(Object[] varVals) {
    int[] retArr = new int[varVals.length];
    for (int i = 0; i < varVals.length; i++) {
      // Check if Object is an Integer or a String, handle appropriately
      if (varVals[i] instanceof Integer) {
        retArr[i] = (Integer) varVals[i];
        // intVars.add((Integer) vars.get(i));
      }
      else if (varVals[i] instanceof String) {
        retArr[i] = Integer.parseInt((String) varVals[i]);
        // intVars.add(Integer.parseInt((String) vars.get(i)));
      }
    }
    return retArr;
  }

  // global running state index
  public int stateCount;
  public int transitionCount;

  // transition objects store the in-place index and the string name
  // for a state's outgoing transition in a single object.
  public class Transition {
    public int prismIndex;
    public int from;
    public int to;
    public double rate;
    public String name;
    public Transition(int prismIndex, String name, int from, int to, double rate) {
      this.prismIndex = prismIndex;
      this.name = name;
      this.from = from;
      this.to = to;
      this.rate = rate;
      transitionCount++;
    }
    public String prismTRA() {
      return (this.from) + " " + (this.to) + " " + (this.rate) + "\n";
    }
  }

  // global variable to store number of state variables
  public int numStateVariables;

  // state objects store the bulk of information about the model
  public class State {
    public int index;
    public int[] stateVars;
    public double totalOutgoingRate;
    public ArrayList<Transition> outgoingTrans;
    public ArrayList<State> nextStates;
    public boolean isTarget;

    public State(Object varVals[]) {
      this.isTarget = false;
      this.index = stateCount;
      stateCount++;
      this.stateVars = getIntVarVals(varVals);
      this.totalOutgoingRate = 0.0;
      this.outgoingTrans = new ArrayList<Transition>();
      this.nextStates = new ArrayList<State>();
      stateList.add(this);
    }

    public State(int varVals[]) {
      this.isTarget = false;
      this.index = stateCount;
      stateCount++;
      this.stateVars = new int[numStateVariables];
      for (int i = 0; i < numStateVariables; i++) {
        this.stateVars[i] = varVals[i];
      }
      this.totalOutgoingRate = 0.0;
      this.outgoingTrans = new ArrayList<Transition>();
      this.nextStates = new ArrayList<State>();
      stateList.add(this);
    }

    // get outgoing rate not captured by notated transitions
    public double getAbsorbingRate() {
      double absorbRate = this.totalOutgoingRate;
      for (int i = 0; i < this.outgoingTrans.size(); i++) {
        absorbRate -= this.outgoingTrans.get(i).rate;
      }
      return absorbRate;
    }

    // State details for .sta files
    // Format is <index>:(<state_var>,<state_var>...)
    public String prismSTA() {
      String temp = (index) + ":(";
      for (int i=0; i<numStateVariables; i++) {
        if (i>0) temp += ",";
        temp += stateVars[i]; 
      }
      return temp + ")\n";
    }

    // State details for .tra files
    // Format is <index>:(<state_var>,<state_var>...)
    public String prismTRA() {
      String temp = "";
      for (int i=0; i<this.outgoingTrans.size(); i++) {
        temp += this.outgoingTrans.get(i).prismTRA(); 
      }
      return temp;
    }

  }

  public ArrayList<State> stateList = new ArrayList<State>();

  public class Path {
    public ArrayList<State> states;
    public ArrayList<String> commutable;
    public Path() {
      this.states = new ArrayList<State>();
      this.commutable = new ArrayList<String>();
    }
  }

  // object to store state variables in the tree structure
  public class StateVarNode {
    public int value;
    public int stateIndex; // index of the first discovered state to have this value
    public ArrayList<StateVarNode> children;
    public StateVarNode() {
      this.value = -1;
      this.children = new ArrayList<StateVarNode>();
    }
    public StateVarNode(int value) {
      this.value = value;
      this.stateIndex = stateCount;
      this.children = new ArrayList<StateVarNode>();
    }
    public void addChild(int value) {
      this.children.add(new StateVarNode(value));
      // maybe sort the list as well eventually?
    }
  }

  // global state variable root is just an empty node
  public StateVarNode StateVarRoot = new StateVarNode();

  // function to check uniqueness and update unique state tree
  public int stateIsUnique(int varVals[]) {
    // loop through all the state variables to see if they exist
    StateVarNode cur = StateVarRoot;
    boolean foundStateVar = false;
    int foundStateIndex = -1;
    for (int stateVar = 0; stateVar < numStateVariables; stateVar++) {
      foundStateVar = false;
      for (int i = 0; i < cur.children.size(); i++) { // new states have 0 kids anyway
        if (cur.children.get(i).value == varVals[stateVar]) {
          cur = cur.children.get(i);
          foundStateVar = true;
          foundStateIndex = cur.stateIndex;
          break;
        }
      }
      if (!foundStateVar) { // if we didn't find it (i.e. state doesn't exist)
        cur.children.add(new StateVarNode(varVals[stateVar]));
        cur = cur.children.get(cur.children.size()-1);
        foundStateIndex = -1;
      }
    }

    return foundStateIndex;
  }

  // save the number of state variables
  public int setNumStateVariables(Prism prism) {
    try {
      // Create a new simulation from the initial state
      SimulatorEngine sim = prism.getSimulator();
      sim.createNewPath();
      sim.initialisePath(null);
      Object varVals[] = sim.getCurrentState().varValues;
      numStateVariables = varVals.length;
      return varVals.length;
    }
    catch (PrismException e) {
			if (DO_PRINT) System.out.println("PrismException Error: " + e.getMessage());
			System.exit(1);
		}
    return -1;
  }

  public void buildAndCommute(Prism prism, String[] transitions, String[] prefix, int depth, Path parentPath, Expression target)
  {
    try {

      if (depth > MAX_DEPTH) {
        if (DO_PRINT) System.out.println("Max recursion depth reached.");
        return;
      }

      // Create a new simulation from the initial state
      SimulatorEngine sim = prism.getSimulator();
      sim.createNewPath();
      sim.initialisePath(null);

      if (DO_PRINT) System.out.println("Prism simulator initialized successfully.");
      if (DO_PRINT) System.out.println("Initial state: " + sim.getCurrentState());
      
      // temporary found transition index variable
      int transitionIndex;
      
      // initialize to the initial state each time
      int currentStateIndex = 0;
      
      // check if we've created an initial state
      if (stateList.size() == 0) {
        if (DO_PRINT) System.out.println("Initial state generated.");
        new State(sim.getCurrentState().varValues);
      }
      
      // update current state to be the model's initial state
      State currentState = stateList.get(0);
      
      // Walk along the prefix to get the new initial state
      int prefixLength = 0;
      if (prefix != null) prefixLength = prefix.length;
      for (int path_tran = 0; path_tran < prefixLength; path_tran++) {

        // start with a fresh transitionIndex
        transitionIndex = -1;
        double newTranRate = -1.0f;
        double totalOutgoingRate = 0.0f;
        // Compare our transition string with available transition strings
        for (int sim_tran = 0; sim_tran < sim.getNumTransitions(); sim_tran++) {
          // Update transitionIndex if we found the desired transition (i.e. names match)
          totalOutgoingRate += sim.getTransitionProbability(sim_tran);
          if (prefix[path_tran].equalsIgnoreCase(sim.getTransitionActionString(sim_tran))) {
            transitionIndex = sim_tran;
            newTranRate = sim.getTransitionProbability(sim_tran);
          }
        }
        // If we never found the correct transitions, report error
        if (transitionIndex == -1) {
          System.out.printf("ERROR: Prefix transition not available from current state: ");
          System.out.println(sim.getCurrentState());
          System.exit(10001);
        }
        // Take the transition
        sim.manualTransition(transitionIndex);
        
        // update the total outgoing rate of the current state
        currentState.totalOutgoingRate = totalOutgoingRate;

        // Check if the state exists yet
        int indexOfFoundState = stateIsUnique(getIntVarVals(sim.getCurrentState().varValues));
        
        // figure out what state to link here
        State stateToAdd = null;
        if (indexOfFoundState == -1) {
          stateToAdd = new State(sim.getCurrentState().varValues);
        }
        else {
          stateToAdd = stateList.get(indexOfFoundState);
          // make sure we haven't already made this transition
          if (currentState.nextStates.contains(stateToAdd)) {
            currentState = stateToAdd;
            continue;
          }
        }
        // add the transition to the discovered state
        currentState.nextStates.add(stateToAdd);
        Transition newTrans = new Transition(transitionIndex, transitions[path_tran], currentState.index, stateToAdd.index, newTranRate);
        currentState.outgoingTrans.add(newTrans);

        // walk along the trace
        currentState = stateToAdd;
        
      } // end walk along path prefix

      if (DO_PRINT) System.out.println("Successfully walked along trace prefix (set of commuted transitions)");

      // Save these states into a path
      Path seedPath = new Path();

      // Set up lists to check commutable transitions
      ArrayList<String> wasEnabled = new ArrayList<String>();
      
      // Add initially enabled transitions to start the commutable check
      for (int sim_tran = 0; sim_tran < sim.getNumTransitions(); sim_tran++) {
        seedPath.commutable.add(sim.getTransitionActionString(sim_tran));
      }

      // Walk along the actual trace, making new states
      // and getting commutable transitions
      for (int path_tran = 0; path_tran < transitions.length; path_tran++) {

        // start with a fresh transitionIndex
        transitionIndex = -1;
        double newTranRate = -1.0f;
        double totalOutgoingRate = 0.0f;
        if (DO_PRINT) System.out.println(sim.getCurrentState());
        // Compare our transition string with available transition strings
        for (int sim_tran = 0; sim_tran < sim.getNumTransitions(); sim_tran++) {
          // Update transitionIndex if we found the desired transition (i.e. names match)
          totalOutgoingRate += sim.getTransitionProbability(sim_tran);
          // if (DO_PRINT) System.out.println("Enabled transition " + sim.getTransitionActionString(sim_tran));
          if (transitions[path_tran].equalsIgnoreCase(sim.getTransitionActionString(sim_tran))) {
            transitionIndex = sim_tran;
            newTranRate = sim.getTransitionProbability(sim_tran);
            if (DO_PRINT) System.out.println("Found transition " + transitions[path_tran]);
          }
        }
        // If we never found the correct transitions, report error
        if (transitionIndex == -1) {
          if (depth > 0) {
            if (DO_PRINT) System.out.printf("WARNING: Trace transition %s not available from current state %s\n", transitions[path_tran], sim.getCurrentState());
            return; // basically just skip this path
          }
          else { // if we're still at the base seed path
          System.out.printf("ERROR: Initial trace transition %s not available from current state %s\n", transitions[path_tran], sim.getCurrentState());
            System.exit(10002);
          }
        }
        // Take the transition
        sim.manualTransition(transitionIndex);
        
        // update the total outgoing rate of the current state
        currentState.totalOutgoingRate = totalOutgoingRate;

        // Check if the state exists yet
        int indexOfFoundState = stateIsUnique(getIntVarVals(sim.getCurrentState().varValues));
        // if (indexOfFoundState == -1) {
        //   System.out.println("Discovered state is unique.");
        // }

        // TODO: Since we're trying to commute, if this doesn't work, return
        
        // figure out what state to link here
        State stateToAdd = null;
        if (indexOfFoundState == -1) {
          stateToAdd = new State(sim.getCurrentState().varValues);
        }
        else {
          stateToAdd = stateList.get(indexOfFoundState);
          // make sure we haven't already made this transition
          if (currentState.nextStates.contains(stateToAdd)) {
            // System.out.println("NEXT STATE ALREADY FOUND");
            seedPath.states.add(currentState);
            currentState = stateToAdd;
            continue;
          }
        }
        // add the transition to the discovered state
        currentState.nextStates.add(stateToAdd);
        Transition newTrans = new Transition(transitionIndex, transitions[path_tran], currentState.index, stateToAdd.index, newTranRate);
        currentState.outgoingTrans.add(newTrans);
        
        // save the current state into the path
        seedPath.states.add(currentState);

        // if we have reached the target 
        if (target.evaluateBoolean(sim.getCurrentState())) {
          if (DO_PRINT) System.out.println("Target Reached");
          stateToAdd.isTarget = true;
          seedPath.states.add(stateToAdd);
        }

        // walk along the trace
        currentState = stateToAdd;
        
        // update commutable transitions based on new state
        wasEnabled = seedPath.commutable;
        seedPath.commutable = new ArrayList<String>();
        for (int sim_tran = 0; sim_tran < sim.getNumTransitions(); sim_tran++) {
          String tempStr = sim.getTransitionActionString(sim_tran);
          if (wasEnabled.contains(tempStr)) {
            seedPath.commutable.add(tempStr);
          }
        }
        
      } // end walk along actual trace

      if (DO_PRINT)  {
        System.out.println("One path explored: ");
        for (int i = 0; i < seedPath.states.size(); i++) {
          System.out.printf("%d ", seedPath.states.get(i).index);
        }
        System.out.println(".");
      }
      // NOTE:
      // commutable transitions are now stored in seedPath.commutable.

      //
      //
      // COMMUTING TRANSITIONS FIRST ALONG THE CURRENT SAVED SEED PATH
      //
      //

      // sim.backtrackTo(seedPath.states.size()-0);
      // System.out.println("Backtrack seedPath.states.size()-0: " + sim.getCurrentState());
      // sim.backtrackTo(seedPath.states.size()-1);
      // System.out.println("Backtrack seedPath.states.size()-1: " + sim.getCurrentState());
      // sim.backtrackTo(seedPath.states.size()-2);
      // System.out.println("Backtrack seedPath.states.size()-2: " + sim.getCurrentState());
      // sim.backtrackTo(seedPath.states.size()-3);
      // System.out.println("Backtrack seedPath.states.size()-3: " + sim.getCurrentState());
      // sim.backtrackTo(seedPath.states.size()-4);



      // for each state in the seed path, backtracking toward the initial state + prefix
      for (int seedIndex = seedPath.states.size()-1; seedIndex >= 0; seedIndex--) {

        // set our current state to the seedpath state
        currentState = seedPath.states.get(seedIndex);
        if (DO_PRINT) System.out.println("currentState " + currentState.prismSTA());
        sim.backtrackTo(seedIndex);
        if (DO_PRINT) System.out.println("sim: " + sim.getCurrentState());
        
        if (DO_PRINT) System.out.println();
        if (DO_PRINT) System.out.println();

        // for each commutable transition
        for (int ctran = 0; ctran < seedPath.commutable.size(); ctran++) {
          // fire the transition
          transitionIndex = -1;
          double newTranRate = -1.0f;
          double totalOutgoingRate = 0.0f;
          // Compare our transition string with available transition strings
          for (int sim_tran = 0; sim_tran < sim.getNumTransitions(); sim_tran++) {
            // Update transitionIndex if we found the desired transition (i.e. names match)
            totalOutgoingRate += sim.getTransitionProbability(sim_tran);
            if (seedPath.commutable.get(ctran).equalsIgnoreCase(sim.getTransitionActionString(sim_tran))) {
              transitionIndex = sim_tran;
              newTranRate = sim.getTransitionProbability(sim_tran);
            }
          }

          // If we never found the correct transitions, report error
          if (transitionIndex == -1) {
            if (DO_PRINT) System.out.printf("WARNING: Commutable transition %s not available from current state %s\n", seedPath.commutable.get(ctran), sim.getCurrentState());
            continue;
          }

          // Take the transition
          sim.manualTransition(transitionIndex);
          
          // update the total outgoing rate of the current state
          currentState.totalOutgoingRate = totalOutgoingRate;
  
          // Check if the state exists yet
          int indexOfFoundState = stateIsUnique(getIntVarVals(sim.getCurrentState().varValues));
          
          // figure out what state to link here
          State stateToAdd = null;
          if (indexOfFoundState == -1) {
            stateToAdd = new State(sim.getCurrentState().varValues);
          }
          else {
            stateToAdd = stateList.get(indexOfFoundState);
            // make sure we haven't already made this transition
            if (currentState.nextStates.contains(stateToAdd)) {
              currentState = stateToAdd;
              continue;
            }
          }

          // add the transition to the discovered state
          currentState.nextStates.add(stateToAdd);

          Transition newTrans = new Transition(transitionIndex, seedPath.commutable.get(ctran), currentState.index, stateToAdd.index, newTranRate);
          currentState.outgoingTrans.add(newTrans);

          if (DO_PRINT) System.out.printf("Added transition %s\n", newTrans.prismTRA() );

      }
    }







      
      // get the length of the prefix
      int prefixLen;
      if (prefix == null) {
        prefixLen = 0;
      }
      else {
        prefixLen = prefix.length;
      }

      // commute if we're within depth bounds
      if (depth < MAX_DEPTH) {
        // for each commutable transition
        for (int ctran = 0; ctran < seedPath.commutable.size(); ctran++) {

          // append the commutable transition to the existing prefix
          String[] newPrefix = new String[prefixLen + 1];
          for (int i = 0; i < prefixLen; i++) {
            newPrefix[i] = prefix[i];
          }
          newPrefix[prefixLen] = seedPath.commutable.get(ctran);
  
          if (DO_PRINT) System.out.println("Recursing into next prefix: " + seedPath.commutable.get(ctran) + " with depth " + (depth+1));
  
          // build a new path and commute
          buildAndCommute(prism, transitions, newPrefix, depth+1, seedPath, target);
        }
      }
      if (DO_PRINT) System.out.println("A path has been successfully commuted");
    }
    catch (PrismException e) {
      if (DO_PRINT) System.out.println("PrismException Error: " + e.getMessage());
      System.exit(1);
    }
  }

  public int setAbsorbingState() {
    // set absorbing state to all -1
    int[] tempAbsorb = new int[numStateVariables];
    int absIndex = stateCount;
    for (int i = 0; i < numStateVariables; i++) {
      tempAbsorb[i] = -1;
    }
    State absorbingState = new State(tempAbsorb);
    double stateAbsorbRate;
    for (int i = 0; i < stateList.size(); i++) {
      stateAbsorbRate = stateList.get(i).getAbsorbingRate();
      if (stateAbsorbRate > 0) {
        stateList.get(i).outgoingTrans.add(new Transition(-1, "ABS", i, absIndex, stateAbsorbRate));
      }
    }
    return absIndex;
  }

  // Top-level function, runs algorithm
  public void run() 
  {
    try
    {

      System.out.println("Welcome to the model commutation tool.");
      // start by resetting the state count
      stateCount = 0;
      
      // give PRISM output a log file
      PrismLog mainLog = new PrismDevNullLog();
      
      // initialise (with an s) PRISM engine
      Prism prism = new Prism(mainLog);
      prism.initialise();
      
      if (DO_PRINT) System.out.println("Prism initialized successfully.");
      // Parse the prism model
      // For now, MODEL_NAME is the hard-coded model file name
      ModulesFile modulesFile = prism.parseModelFile(new File(MODEL_NAME));
      if (DO_PRINT) System.out.println("Prism model parsed successfully.");
      
      // Load the prism model for checking
      prism.loadPRISMModel(modulesFile);
      if (DO_PRINT) System.out.println("Prism model loaded successfully.");
      
      // Load the model into the simulator engine
      prism.loadModelIntoSimulator();
      if (DO_PRINT) System.out.println("Prism model loaded into simulator successfully.");
      
      // Load the target property
      // TODO: Read the file
      Expression target = prism.parsePropertiesString("gbg = 50").getProperty(0);
      // Expression target = prism.parsePropertiesString(targetString).getProperty(0);
      
      System.out.println("Prism model loaded succesfully.");
      // set the number of state variables for the model
      setNumStateVariables(prism);
      if (DO_PRINT) System.out.printf("Number of state variables: %d\n", numStateVariables);

      // Read in the first line of the trace as a string
      // For now, the trace must go in forprism.trace (handled in python script)
			FileReader fr = new FileReader(TRACE_LIST_NAME);
			BufferedReader br = new BufferedReader(fr);
      String trace;
      String[] transitions;
      int num_transitions;

      int numPaths = 0;

      // For each input trace
      while (true) {

        
        // read in a single seed trace
        trace = br.readLine();
        if (trace == null) break;
        
        numPaths++;

        if (numPaths % 25 == 0) {
          System.out.printf("Processed %d paths with a state count of %d\n", numPaths, stateCount);
        }
        
        // TODO: POSSIBLY TERMINATE BASED ON DELTA-STATECOUNT?
        // i.e. if stateCount hasn't changed in many paths, terminate
        
        // break the trace into an array of individual transitions
        transitions = trace.split("\\s+");
        for (int i = 0; i < transitions.length; i++) {
          transitions[i] = String.format("[%s]", transitions[i]);
        }
        num_transitions = transitions.length;
        
        if (DO_PRINT) System.out.println("Read trace with " + num_transitions + " transitions.");
        
        // construct states with transitions along the trace
        // then commute along the generated path
        buildAndCommute(prism, transitions, null, 0, null, target);

      }

      System.out.printf("Processed %d paths with a state count of %d\n", numPaths, stateCount);

      System.out.println("Establishing an absorbing state.");
      
      int absorbIndex = setAbsorbingState();

      System.out.println("Begin printing model files.");

      // Initialize label string
      // TODO: Play with deadlock vs sink
      String labStr = "0=\"init\" 1=\"sink\"\n0: 0\n";
      labStr += (absorbIndex + ": 1");
      
      String labStrS = "#DECLARATION\ninit absorb target\n#END\n0 init\n";

      // Initialize transition string
      String traStr = "";
      String traStrS = "ctmc\n";
      traStr += ((stateList.size()) + " " + transitionCount + "\n");

      // Initialize state string
      String staStr = "";
      staStr += "(";

      // Create a new simulation from the initial state, to get the var names
      SimulatorEngine sim = prism.getSimulator();
      sim.createNewPath();
      sim.initialisePath(null);
      // Get variable names for first line of .sta file
      int vari = 0;
      String varName = "";
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

      for (int a = 0; a < stateList.size(); a++) {
        staStr += stateList.get(a).prismSTA();
        traStr += stateList.get(a).prismTRA();
        traStrS += stateList.get(a).prismTRA();
        if (stateList.get(a).isTarget) {
          labStrS += (stateList.get(a).index + " target\n");
        }
      }

      traStrS += (absorbIndex + " " + absorbIndex + " 0.0");
      labStrS += (absorbIndex + " absorb");

      System.out.println("Now printing " + numPaths + " paths to PRISM model files.");
      
      // Write the state file to prism.sta
      BufferedWriter staWriter = new BufferedWriter(new FileWriter("prism.sta"));
      staWriter.write(staStr.trim());
      staWriter.close();
      
      // Write the transition file to prism.tra
      BufferedWriter traWriter = new BufferedWriter(new FileWriter("prism.tra"));
      traWriter.write(traStr.trim());
      traWriter.close();
      
      // Write the label file to prism.lab
      BufferedWriter labWriter = new BufferedWriter(new FileWriter("prism.lab"));
      labWriter.write(labStr.trim());
      labWriter.close();
      
      System.out.println("Now printing " + numPaths + " paths to STORM model files.");
      
      // Write the transition file to storm2.tra
      BufferedWriter traWriterS = new BufferedWriter(new FileWriter("storm2.tra"));
      traWriterS.write(traStrS.trim());
      traWriterS.close();

      // Write the label file to storm2.lab
      BufferedWriter labWriterS = new BufferedWriter(new FileWriter("storm2.lab"));
      labWriterS.write(labStrS.trim());
      labWriterS.close();

      System.out.println("Finished! Processed " + numPaths + " paths.");

      // close PRISM
      prism.closeDown();

    }
    // Catch exceptions and give user the info
    catch (PrismException e) {
			if (DO_PRINT) System.out.println("PrismException Error: " + e.getMessage());
			System.exit(1);
		}
    catch (IOException e) {
      if (DO_PRINT) System.out.println("IOException Error: " + e.getMessage());
			System.exit(1);
    }

  }

}
