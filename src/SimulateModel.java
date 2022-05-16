package simulate;

import java.io.File;
import java.io.FileNotFoundException;

import java.io.BufferedReader;
import java.io.FileReader;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;

import parser.Values;
import parser.ast.Expression;
import parser.ast.ModulesFile;

import prism.Prism;
import prism.PrismDevNullLog;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismPrintStreamLog;

import simulator.SimulatorEngine;


/**
Produced by Landon Taylor in early 2022
Based in large part on prism-api/src/SimulateModel.java
*/

public class SimulateModel
{
  public static void main(String[] args)
  {
    new SimulateModel().run();
  }

  public void run()
  {
    try {
      // give PRISM output a log file
      PrismLog mainLog = new PrismDevNullLog();

      // initialise (with an s) PRISM engine
      Prism prism = new Prism(mainLog);
      prism.initialise();

      // parse the prism model
      // For now, model.sm is the model file.
      // TODO: use the command line argument
      ModulesFile modulesFile = prism.parseModelFile(new File("model.sm"));

      // load the prism model
      prism.loadPRISMModel(modulesFile);

      // if the model has unknown constant values, deal with that here.
      // for now, we assume NO UNDEFINED CONSTANTS

      // load the model into the simulator
      prism.loadModelIntoSimulator();
      SimulatorEngine sim = prism.getSimulator();

      // follow the transitions
		
      // Read in the first line of the trace as a string
			FileReader fr = new FileReader("forprism.trace");
			BufferedReader br = new BufferedReader(fr);
			String x;
			x = br.readLine();

      // create a new ivy file with that initial state

      // Look for CHANGE_IVY_INITIAL_STATE in first line
      // to indicate we want a new IVy model
      if (x.contains("CHANGE_IVY_INITIAL_STATE")) {
        // Break the string into a transition set
			  String[] tr_st = x.split("\\s+"); 
        // create a new path
        sim.createNewPath();
        sim.initialisePath(null);

        int index;
        // walk along the path
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
        // print the full trace
        // sim.getPathFull().exportToLog(new PrismPrintStreamLog(System.out), true, ",", null);
        // print the state (hopefully)
        // System.out.println(sim.getPath());
        System.out.println(sim.getCurrentState());
      }
      else if (x.contains("BUILD_MODEL")) {
        // Break the string into a transition set
			  String[] tr_st = x.split("\\s+");
        System.out.println("Split: ");
        System.out.println(tr_st);
        // create a new path
        sim.createNewPath();
        sim.initialisePath(null);

        // set up the state graph in a list for now
        // todo: currently assuming that there are no duplicate states
        // todo: currently assuming only one commuted transition

        ArrayList<int[]> states = new ArrayList<int[]>();

        // set up a rolling state index to add new states
        int rollingStateIndex = 0;

        // Make the absorbing state
        // TODO: For now, we just hard-coded the absorbing state to match 
        // the six-reaction model. Eventually fix this.

        states.add(new int[]{-1,-1,-1,-1,-1,-1});

        /*
          Order of path analysis:
          0) Build absorbing state, gathering its probability at each step
          1) Go along the path firing t_alpha from s_i
          2) Go along the original path, getting the probability of t_alpha
        */

        int index;
        // walk along the path (0 is BUILD MODEL, 1 is commuted transition)
        for (int tdx=2; tdx < tr_st.length; tdx++) {
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
        // print the full trace
        // sim.getPathFull().exportToLog(new PrismPrintStreamLog(System.out), true, ",", null);
        // print the state (hopefully)
        // System.out.println(sim.getPath());
        System.out.println("sim.getCurrentState");
        System.out.println(sim.getCurrentState());
        System.out.println(sim.getCurrentState().getClass().getName());
      }
      // if it's just a regular model
      else {
        // Break the string into a transition set
        String[] tr_st=x.split("\\s+"); 

        FileReader fr_p = new FileReader("model.csl");
        BufferedReader br_p = new BufferedReader(fr_p);
        String x_p;
        x_p = br_p.readLine();

        // create a new path
        sim.createNewPath();

        // sim.createNewOnTheFlyPath(); // recommended for efficiency
        sim.initialisePath(null);

        // Take each transition and collect the rates
        int index;
        double pathProbability = 1.0;
        double totalRate = 0.0;
        System.out.printf("%d length\n", tr_st.length);

        for (int tdx=0; tdx < tr_st.length; tdx++) {
          index = 0;
          totalRate = 0.0;
          for (int idx=0; idx < sim.getNumTransitions(); idx++) {
            // System.out.printf("tr %d: %s %f\n", idx, sim.getTransitionActionString(idx), sim.getTransitionProbability(idx));
            System.out.printf("%s ", sim.getTransitionActionString(idx).replace("[","").replace("]",""));
            totalRate += sim.getTransitionProbability(idx);
          } // try combining these
          for (int idx=0; idx < sim.getNumTransitions(); idx++) {
            String s1 = String.format("[%s]",tr_st[tdx]);
            String s2 = sim.getTransitionActionString(idx);
            if (s1.equalsIgnoreCase(s2)) {
                index = idx;
                break;
            }
          }
          double transition_probability = sim.getTransitionProbability(index) / totalRate;
          // System.out.printf("\n======= tr %s (%d) %e ===========\n\n", tr_st[tdx], index, transition_probability);
          System.out.printf("\n");
          pathProbability *= transition_probability;
          sim.manualTransition(index);
        }

        Expression target = prism.parsePropertiesString(x_p).getProperty(0);

        if (!target.evaluateBoolean(sim.getCurrentState())) {
          System.out.printf(">> target state not reached ERR_TAR_NO_RCH\n>> Probability not counted\n");
          sim.getPathFull().exportToLog(new PrismPrintStreamLog(System.out), true, ",", null);
        }
        else {
          System.out.printf(">> Path Reaches Target :)\n");
          System.out.printf("pathProbability %e\n", pathProbability);
        }

        // System.out.println(sim.getPath());
        
        // sim.getPathFull().exportToLog(new PrismPrintStreamLog(System.out), true, ",", null);


        // get path probability, dummy attempt
        // System.out.println("Path Probability:");
        // double pathProbability = 1.0;
        // for (int idx=0; idx<sim.getNumTransitions(); idx++) {
        //   System.out.println(pathProbability + "*=" + sim.getTransitionProbability(idx));
        //   pathProbability *= sim.getTransitionProbability(idx);
        //   System.out.println(sim.getTransitionProbability(idx));
        //   System.out.println(pathProbability);
        //   System.out.println("");
        // }
        

        // close PRISM
        prism.closeDown();
      }
    } 
    catch (FileNotFoundException e) {
			System.out.println("FileNotFound Error: " + e.getMessage());
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