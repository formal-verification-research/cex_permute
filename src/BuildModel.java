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
  public static final int MAX_DEPTH = 2;

  // turn off printing to save time
  public static final boolean DO_PRINT = false;

  // static model name
  public static final String MODEL_NAME = "model.sm";
  public static final String TRACE_LIST_NAME = "forprism.trace";

  // By default, call BuildModel().run()
  public static void main(String[] args)
  {
    new BuildModel().run();
  }

  public class State {
    public int index;
    public int[] stateVars;
    public double totalOutgoingRate;
    public String[] outgoingTrans;
    public State[] nextStates;
  }






  public void buildAndCommute(Prism prism, String[] transitions, String[] prefix, Model model)
  {
    try {

      // Create a new simulation from the initial state
      SimulatorEngine sim = prism.getSimulator();
      sim.createNewPath();
      sim.initialisePath(null);

      int index;

      // Walk along the prefix to get the new initial state
      for (int path_tran = 0; path_tran < prefix.length; path_tran++) {
        // start with a fresh index
        index = -1;
        // Compare our transition string with available transition strings
        for (int sim_tran = 0; sim_tran < sim.getNumTransitions(); sim_tran++) {
          // Update index if we found the desired transition (i.e. names match)
          if (prefix[path_tran].equalsIgnoreCase(sim.getTransitionActionString(sim_tran))) {
            index = sim_tran;
            break;
          }
        }
        // If we never found the correct transitions, report error
        if (index == -1) {
          System.out.println("ERROR: Prefix transition not available from current state.");
          System.exit(1);
        }
        // Take the transition
        sim.manualTransition(index);
      } // end walk along path prefix


      // Walk along the actual trace, making new states
      // TODO: Add state information
      for (int path_tran = 0; path_tran < transitions.length; path_tran++) {
        // start with a fresh index
        index = -1;
        // Compare our transition string with available transition strings
        for (int sim_tran = 0; sim_tran < sim.getNumTransitions(); sim_tran++) {
          // Update index if we found the desired transition (i.e. names match)
          if (transitions[path_tran].equalsIgnoreCase(sim.getTransitionActionString(sim_tran))) {
            index = sim_tran;
            break;
          }
        }
        // If we never found the correct transitions, report error
        if (index == -1) {
          System.out.println("ERROR: Trace transition not available from current state.");
          System.exit(1);
        }
        // Take the transition
        sim.manualTransition(index);


      } // end walk along path prefix







    }
    catch (PrismException e) {
      if (DO_PRINT) System.out.println("PrismException Error: " + e.getMessage());
      System.exit(1);
    }
  }

  // Top-level function, runs algorithm
  public void run() 
  {
    try
    {

      // give PRISM output a log file
      PrismLog mainLog = new PrismDevNullLog();
  
      // initialise (with an s) PRISM engine
      Prism prism = new Prism(mainLog);
      prism.initialise();

      // Parse the prism model
      // For now, MODEL_NAME is the hard-coded model file name
      ModulesFile modulesFile = prism.parseModelFile(new File(MODEL_NAME));

      // Load the prism model for checking
      prism.loadPRISMModel(modulesFile);

      // Load the model into the simulator engine
      prism.loadModelIntoSimulator();

      // Initialize the model
      // TODO: Make model object
      Model model = new Model();

      // Read in the first line of the trace as a string
      // For now, the trace must go in forprism.trace (handled in python script)
			FileReader fr = new FileReader(TRACE_LIST_NAME);
			BufferedReader br = new BufferedReader(fr);
      String trace;
      String[] transitions;
      int num_transitions;

      while (true) {

        // read in a single seed trace
        trace = br.readLine();
        if (trace == null) break;

        // break the trace into an array of individual transitions
        transitions = x_transitions.split("\\s+");
        num_transitions = transitions.length;

        // construct states with transitions along the trace
        // then commute along the generated path
        buildAndCommute(prism, transitions);

      }

      // Configure the absorbing state
      model.addAbsorbingState();

      // Print to files
      SimulatorEngine sim = prism.getSimulator();
      model.exportFiles(sim);

      // close PRISM
      prism.closeDown();

    }
    // Catch exceptions and give user the info
    catch (PrismException e) {
			if (DO_PRINT) System.out.println("PrismException Error: " + e.getMessage());
			System.exit(1);
		}
    // catch (IOException e) {
    //   if (DO_PRINT) System.out.println("IOException Error: " + e.getMessage());
		// 	System.exit(1);
    // }


  }

}