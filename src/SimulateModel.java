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
public class SimulateModel
{

  // By default, call SimulateModel().run()
  public static void main(String[] args)
  {
    new SimulateModel().run();
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
    public int[] vars; // State variable values
    public double totalRate; // Total outgoing rate at this state
    public ArrayList<Transition> outgoing; // Set of saved outgoing transitions

    // Initialize a state, nothing fancy here
    public State(int index, int[] vars, double totalRate) {
      this.index = index;
      this.vars = vars;
      this.totalRate = totalRate;
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
  }

  // Main run function - runs every call
  public void run()
  {
    try {
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
      SimulatorEngine sim = prism.getSimulator();
		
      // Read in the first line of the trace as a string
      // For now, the trace must go in forprism.trace (done in python script)
			FileReader fr = new FileReader("forprism.trace");
			BufferedReader br = new BufferedReader(fr);
			String x;
			x = br.readLine();


      // Look for CHANGE_IVY_INITIAL_STATE in first line
      //  to indicate we want to fire a couple of transitions, then
      //  print the resulting state variable information
      if (x.contains("CHANGE_IVY_INITIAL_STATE")) {

        // Break the string into a transition set
			  String[] tr_st = x.split("\\s+"); 

        // create a new path
        sim.createNewPath();
        sim.initialisePath(null);

        // Walk along the given path
        int index;
        for (int tdx=1; tdx < tr_st.length; tdx++) {
          index = 0;
          for (int idx=0; idx < sim.getNumTransitions(); idx++) {
            String s1 = String.format("[%s]",tr_st[tdx]);
            String s2 = sim.getTransitionActionString(idx);
            if (s1.equalsIgnoreCase(s2)) {
                index = idx;
                break;
            }
          }
          sim.manualTransition(index);
        }

        // Print the current state
        System.out.println(sim.getCurrentState());

        // close PRISM
        prism.closeDown();
        
      }


      // Look for BUILD_MODEL in first line
      // to indicate we want to build .sta, .tra, and .lab files
      else if (x.contains("BUILD_MODEL")) {
        // Break the input file into its parts 
        // File follows this format:
        // BUILD_MODEL
        // COMMUTABLE (tab-separated)
        // PATH (tab-separated)
        
        // Read in the commutable transition line
        String x_commute = br.readLine();
        String[] commute = x_commute.split("\\s+");
        
        // Print the commutable transition list to the user
        System.out.println("Commutable Transitions seen by API: ");
        for (int cmx=0; cmx < commute.length; cmx++) {
          System.out.println(String.format("%d _%s_", cmx, commute[cmx]));
        }

        // Read in the seed path line
        String x_path = br.readLine();
			  String[] tr_st = x_path.split("\\s+");
        
        // Let n be the length of the seed path (in STATES, not transitions)
        int n = tr_st.length + 1;
        System.out.println("n (length of seed path) = " + n);

        // Print seed path with indices, for debugging
        // System.out.println("Seed Path with Indices: ");
        // for (int tdx=0; tdx < tr_st.length; tdx++) {
        //   System.out.println(String.format("%d [%s]", tdx, tr_st[tdx]));
        // }

        // Create a new path to do our construction
        sim.createNewPath();
        sim.initialisePath(null);
        
        // TODO: currently assuming that there are no duplicate states
        // TODO: currently assuming only one commuted transition

        // Make an ArrayList of states in our model
        ArrayList<State> states = new ArrayList<State>();

        // Make an ArrayList of commuted states to cross-check the 
        // independent transition attribute
        ArrayList<State> commutedStates = new ArrayList<State>();      
        
        // Keep a rolling state index while we add our states
        // This makes sure every state gets a fresh index
        int rollingStateIndex = 0;
        int transitionCount = 0;

        // Make a dummy state to fill the spot of State 0 (helps make math easier)
        states.add(new State(rollingStateIndex, new int[]{-5,-5,-5,-5,-5,-5,-5}, 0.0));
        
        // Set the index of the initial state to 1 (explicitly)
        rollingStateIndex = 1;

        // Make the initial state using the simulator's current (unmodified) state
        Object[] tl = sim.getCurrentState().varValues;
        int[] varv = new int[tl.length];
        for (int i = 0; i < tl.length; i++) {
          // Check if Object is an Integer or a String, handle appropriately
          if (tl[i] instanceof Integer) {
            varv[i] = (Integer) tl[i];
          }
          else if (tl[i] instanceof String) {
            varv[i] = Integer.parseInt((String) tl[i]);
          }
        }
        // TODO: Don't hard-code the varv array in
        // TODO: Make it work for any array size.

        // Add the initial state to both state arrays
        states.add(new State(rollingStateIndex, new int[]{varv[0],varv[1],varv[2],varv[3],varv[4],varv[5],varv[6]}, 0.0));
        commutedStates.add(new State(rollingStateIndex, new int[]{varv[0],varv[1],varv[2],varv[3],varv[4],varv[5],varv[6]}, 0.0));

        // Walk along the original path, building states as we go
        int index;
        for (int tdx=0; tdx < tr_st.length; tdx++) {
          index = 0;
          // get the total rate and store it in states
          for (int idx=0; idx < sim.getNumTransitions(); idx++) {
            states.get(rollingStateIndex).addRate(sim.getTransitionProbability(idx));
            // System.out.println(String.format("Available Transition Here: %s (%d)",sim.getTransitionActionString(idx), idx));
          } 

          // get the value for the s_k' after firing t_alpha to cross-check later
          for (int idx=0; idx < sim.getNumTransitions(); idx++) {
            String s1 = String.format("[%s]",commute[0]);
            String s2 = sim.getTransitionActionString(idx);
            if (s1.equalsIgnoreCase(s2)) {
                index = idx;
                break;
            }
          }
          
          // Add the current transition to the current state and increment the transition counter
          // Adds t_alpha in a way that we can remove it later if needed
          states.get(rollingStateIndex).addTransition(rollingStateIndex+n, sim.getTransitionProbability(index), index, sim.getTransitionActionString(index));
          transitionCount++;
          
          // Fire the transition
          sim.manualTransition(index);
          
          // Get variable values to build the state
          Object[] templist_c = sim.getCurrentState().varValues;
          int[] vval_c = new int[templist_c.length]; // vval_c for varValues
          for (int i = 0; i < templist_c.length; i++) {
            // Check if Object vval_c is an Integer or a String
            if (templist_c[i] instanceof Integer) {
              vval_c[i] = (Integer) templist_c[i];
            }
            else if (templist_c[i] instanceof String) {
              vval_c[i] = Integer.parseInt((String) templist_c[i]);
            }
          }
          commutedStates.add(new State(-2, new int[]{vval_c[0],vval_c[1],vval_c[2],vval_c[3],vval_c[4],vval_c[5],vval_c[6]}, 0.0));

          // Backtrack once to the original path
          sim.backtrackTo(rollingStateIndex-1);
          

          // Get the index of the next transition along the seed path
          index=0;
          for (int idx=0; idx < sim.getNumTransitions(); idx++) {
            String s1 = String.format("[%s]",tr_st[tdx]);
            String s2 = sim.getTransitionActionString(idx);
            if (s1.equalsIgnoreCase(s2)) {
                index = idx;
                break;
            }
          }

          // todo: modularize this so much better oh my goodness

          // Get the current state along the seed path, then add the 
          //  next transition along the seed path
          states.get(rollingStateIndex).addTransition(rollingStateIndex+1, sim.getTransitionProbability(index), index, sim.getTransitionActionString(index));
          transitionCount++;

          // Fire the next transition along the seed path
          sim.manualTransition(index);
          
          // Add to the state index
          rollingStateIndex++;
          
          // Get the variable values at the current state
          //  (found at parser->State.java, line 41")
          Object[] templist = sim.getCurrentState().varValues;
          int[] vv = new int[templist.length]; // vv for varValues
          for (int i = 0; i < templist.length; i++) {
            // Check if Object in varValues is an Integer or a String
            //  then parse the (integer) value into the vv array
            if (templist[i] instanceof Integer) {
              vv[i] = (Integer) templist[i];
            }
            else if (templist[i] instanceof String) {
              vv[i] = Integer.parseInt((String) templist[i]);
            }
          }
          // Add the state to the list of states
          // TODO: Don't hard-code the array indices somehow
          states.add(new State(rollingStateIndex, new int[]{vv[0],vv[1],vv[2],vv[3],vv[4],vv[5],vv[6]}, 0.0));
        }


        // SECOND LOOP -- COMMUTED PATH // SECOND LOOP -- COMMUTED PATH
        // SECOND LOOP -- COMMUTED PATH // SECOND LOOP -- COMMUTED PATH
        // SECOND LOOP -- COMMUTED PATH // SECOND LOOP -- COMMUTED PATH
        // SECOND LOOP -- COMMUTED PATH // SECOND LOOP -- COMMUTED PATH
        // SECOND LOOP -- COMMUTED PATH // SECOND LOOP -- COMMUTED PATH

        // Walk along the commuted path to verify the commuted path transitions
        //  yield a correct path to a target state

        // Create a new path called sim2 to handle just these commuted transitions
        SimulatorEngine sim2 = prism.getSimulator();
        sim2.createNewPath();
        
        // Go back to initial state
        sim2.initialisePath(null);

        // Get the index of t_alpha
        index = 0;
        for (int idx=0; idx < sim2.getNumTransitions(); idx++) {
          // TODO: Eventually do each commute[k]
          String s1 = String.format("[%s]",commute[0]);
          String s2 = sim2.getTransitionActionString(idx);
          if (s1.equalsIgnoreCase(s2)) {
              index = idx;
              break;
          }
        }
        
        // Fire the commutable transition from the initial state
        //  to get the "commuted path initial state"
        sim2.manualTransition(index);

        // Add to the (existing) state index counter to ensure uniqueness
        rollingStateIndex++;

        // Get the variable values in the current state
        //  (found at parser->State.java, line 41");
        Object[] templist = sim2.getCurrentState().varValues;
        int[] vv = new int[templist.length]; // vv for varValues
        for (int i = 0; i < templist.length; i++) {
          // Check if Object vv is an Integer or a String
          if (templist[i] instanceof Integer) {
            vv[i] = (Integer) templist[i];
          }
          else if (templist[i] instanceof String) {
            vv[i] = Integer.parseInt((String) templist[i]);
          }
        }
        // Add the first COMMUTED TRANSITION state (state n+1) to the list
        states.add(new State(rollingStateIndex, new int[]{vv[0],vv[1],vv[2],vv[3],vv[4],vv[5],vv[6]}, 0.0));

        // Loop along the seed path transitions to follow the sequence
        for (int tdx=0; tdx < tr_st.length; tdx++) { 
          index = 0;
          // Get the total rate from the outgoing transitions
          for (int idx=0; idx < sim2.getNumTransitions(); idx++) {
            states.get(rollingStateIndex).addRate(sim2.getTransitionProbability(idx));
          } 
          // Get the index of the next transition in the seed sequence
          //  (The loops are separate because we want to get total probability;
          //    we stop the next loop once we find our transition.)
          for (int idx=0; idx < sim2.getNumTransitions(); idx++) {
            String s1 = String.format("[%s]",tr_st[tdx]);
            String s2 = sim2.getTransitionActionString(idx);
            if (s1.equalsIgnoreCase(s2)) {
                index = idx;
                break;
            }
          }
          
          // Add the discovered seed transition to the state
          states.get(rollingStateIndex).addTransition(rollingStateIndex+1, sim2.getTransitionProbability(index), index, sim2.getTransitionActionString(index));
          
          // Increment the transition count
          transitionCount++;

          // Fire the transition from the seed path sequence
          sim2.manualTransition(index);

          // Increment the rolling state count
          rollingStateIndex++;
          
          // Get the state variables at this new state
          Object[] templist1 = sim2.getCurrentState().varValues;
          int[] vv1 = new int[templist1.length]; // vv1 for varValues
          for (int i = 0; i < templist1.length; i++) {
            // Get value if it's an integer or a string
            if (templist1[i] instanceof Integer) {
              vv1[i] = (Integer) templist1[i];
            }
            else if (templist1[i] instanceof String) {
              vv1[i] = Integer.parseInt((String) templist1[i]);
            }
          }
          // Add the new state to the state list
          states.add(new State(rollingStateIndex, new int[]{vv1[0],vv1[1],vv1[2],vv1[3],vv1[4],vv1[5],vv1[6]}, 0.0));
        }

        // Check that t_alpha truly is independent
        //  i.e. check that firing t_a -> t_b yields an identical state
        //  to firing t_b -> t_a. See Principles of Model Checking (8.1)
        boolean commutedCheck = true;
        for (int k = 1; k < commutedStates.size(); k++) {
          if (!states.get(n+k).equals(commutedStates.get(k))) {
            System.out.println("STATES NOT EQUAL. DETAILS ON NEXT LINE.");
            System.out.println("State " + (n+k) + " does not equal commuted state " + k);
            commutedCheck = false;
          }
          else {
            System.out.println("State " + (n+k) + " equals commuted state " + k);
          }
        }

        // if the transition was not independent, remove t_alpha at that location.
        // todo: check that this works. for now, the six_reaction model passes the check
          // so there's no need to worry about removing t_alpha when it's dependent.
        if (!commutedCheck) {
          System.out.println("ERROR: TRANSITION WAS NOT ACTUALLY INDEPENDENT!!!");
          for (int i = 1; i <= n; i++) {
            for (int j = 0; j < states.get(i).outgoing.size(); j++) {
              if (states.get(i).outgoing.get(j).transitionName.contains(commute[0])) {
                states.get(i).outgoing.remove(j);
              }
            }
          }
        }

        // Configure the absorbing state
        rollingStateIndex++;
        int absorbIndex = rollingStateIndex;
        states.add(new State(absorbIndex, new int[]{-1,-1,-1,-1,-1,-1,-1}, 0.0));

        // Get the absorbing state transition information
        System.out.println("Absorbing Index: " + absorbIndex);
        double absorbRate = 0.0;
        // Loop from state 1 to the last real state
        for (int i = 1; i < states.size()-1; i++) {
          // Get the absorbing rate at each state
          absorbRate = states.get(i).getAbsorbingRate();
          // Add a new transition with the absorbing rate going to the 
          //  absorbing state
          // TODO: Skip this if the absorbing rate is 0
          states.get(i).addTransition(absorbIndex, states.get(i).getAbsorbingRate(), -1, "ABSORB");
          transitionCount++;
        }

        // Initialize transition string
        String traStr = "";
        traStr += ((states.size()-1) + " " + transitionCount + "\n");

        // Initialize state string
        String staStr = "";
        staStr += "(";

        // Initialize label string
        String labStr = "0=\"init\" 1=\"sink\"\n0: 0\n";
        labStr += (absorbIndex + ": 1");
        
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

        // Write the state file to model.sta
        BufferedWriter staWriter = new BufferedWriter(new FileWriter("model.sta"));
        staWriter.write(staStr);
        staWriter.close();

        // Write the transition file to model.tra
        BufferedWriter traWriter = new BufferedWriter(new FileWriter("model.tra"));
        traWriter.write(traStr);
        traWriter.close();

        // Write the label file to model.lab
        BufferedWriter labWriter = new BufferedWriter(new FileWriter("model.lab"));
        labWriter.write(labStr);
        labWriter.close();

        // Alert user of successful termination
        System.out.println("Model built. PRISM API ended successfully.");

        // Close PRISM
        prism.closeDown();

      }


      // Default case. No flag indicates we want to simulate a single path 
      //  and get its probability.
      else {
        // Break the string into a transition set
        String[] tr_st=x.split("\\s+"); 

        // Read in the CSL property to check if it satisfies the property
        FileReader fr_p = new FileReader("model.csl");
        BufferedReader br_p = new BufferedReader(fr_p);
        String x_p;
        x_p = br_p.readLine();

        // Create a new path
        sim.createNewPath();
        sim.initialisePath(null);
        // sim.createNewOnTheFlyPath(); is recommended for efficiency
        //  but it has not given correct results

        // Get the index and rate of each transition, then follow the path
        int index;
        double pathProbability = 1.0;
        double totalRate = 0.0;
        for (int tdx=0; tdx < tr_st.length; tdx++) {
          index = 0;
          totalRate = 0.0;

          // Get the total rate in a separate loop (because of the break)
          for (int idx=0; idx < sim.getNumTransitions(); idx++) {
            totalRate += sim.getTransitionProbability(idx);
          } 

          // Get the index of the transition to fire
          for (int idx=0; idx < sim.getNumTransitions(); idx++) {
            String s1 = String.format("[%s]",tr_st[tdx]);
            String s2 = sim.getTransitionActionString(idx);
            if (s1.equalsIgnoreCase(s2)) {
                index = idx;
                break;
            }
          }
          // Get the probability (not rate) of the transition to fire
          double transition_probability = sim.getTransitionProbability(index) / totalRate;
          
          // If the probability is 0, alert the user
          if (transition_probability == 0.0f) {
            System.out.println(String.format("ZERO PROBABILITY FOR TRANSITION %s (%d)", sim.getTransitionActionString(index), index));
          }

          // Multiply probability along the path
          pathProbability *= transition_probability;
          
          // Alert the user of the probability of the fired transition
          System.out.println(String.format("FIRED %s (%d) with probability %e (total %e)", sim.getTransitionActionString(index), index, transition_probability, totalRate));
          
          // Fire the transition
          sim.manualTransition(index);
        }

        // Set up a target expression from the CSL property
        Expression target = prism.parsePropertiesString(x_p).getProperty(0);

        // Check if the current state (final state) satisfies the CSL property
        if (target.evaluateBoolean(sim.getCurrentState())) {
          System.out.printf(">> Path Reaches Target :)\n");
          System.out.printf("pathProbability %e\n", pathProbability);
        }
        // If it does not, alert the user and don't count the probability.
        else {
          System.out.printf(">> target state not reached ERR_TAR_NO_RCH\n>> Probability not counted\n");
          sim.getPathFull().exportToLog(new PrismPrintStreamLog(System.out), true, ",", null);
        }       

        // close PRISM
        prism.closeDown();
      }


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