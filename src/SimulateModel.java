package simulate;

import java.io.File;
import java.io.FileNotFoundException;

import java.io.BufferedReader;
import java.io.FileReader;

import java.io.BufferedWriter;
import java.io.FileWriter;

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

  public class Transition {
    public int from;
    public int to;
    public int transitionIndex = 0;
    public String transitionName = "t";
    public double rate;

    public Transition(int f, int t, double r, int i, String n) {
      this.from = f;
      this.to = t;
      this.rate = r;
      this.transitionIndex = i;
      this.transitionName = n;
    }

    @Override
    public String toString() {
      return this.from + " " + this.to + " " + this.rate + " (" + this.transitionIndex + "_" + this.transitionName;
    }

    public String prism() {
      // subtract 1 from the indices because of the prism quirk
      return (this.from-1) + " " + (this.to-1) + " " + this.rate;
    }

  }

  public class State {
    public int index;
    public int[] vars;
    public double totalRate;
    public ArrayList<Transition> outgoing;

    public State(int index, int[] vars, double totalRate) {
      this.index = index;
      this.vars = vars;
      this.totalRate = totalRate;
      this.outgoing = new ArrayList<Transition>();
    }

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

    public String prism() {
      String temp = (index-1) + ":(";
      for (int i=0; i<vars.length; i++) {
        if (i>0) temp += ",";
        temp += vars[i]; 
      }
      return temp + ")";
    }

    public double addRate(double p) {
      this.totalRate += p;
      return this.totalRate;
    }

    public boolean equals(State s) {
      if (this.vars.length != s.vars.length) return false;
      for (int i=0; i<this.vars.length; i++) {
        if (this.vars[i] != s.vars[i]) return false;
      }
      return true;
    }

    public void addTransition(int to, double rate, int transitionIndex, String transitionName) {
      outgoing.add(new Transition(this.index, to, rate, transitionIndex, transitionName));
      // System.out.println("Added Transition " + outgoing.get(outgoing.size()-1) + " to " + this);

      // if (transitionName.contains("two")) {
      //   System.out.println("CONTAINS TWO (WHAT THE HECK):");
      // }
    }

    // get the rate going into the absorbing state
    public double getAbsorbingRate() {
      double absorbRate = this.totalRate;
      for (int i = 0; i < outgoing.size(); i++) {
        absorbRate -= outgoing.get(i).rate;
      }
      return absorbRate;
    }
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
        // Break the input file into its parts 
        // BUILD_MODEL
        // COMMUTABLE (tab-sep)
        // PATH (tab-sep)
        
        String x_commute = br.readLine();
        String[] commute = x_commute.split("\\s+");
        
        System.out.println("Commutable Transitions seen by API: ");
        for (int cmx=0; cmx < commute.length; cmx++) {
          System.out.println(String.format("%d _%s_", cmx, commute[cmx]));
        }

        String x_path = br.readLine();
			  String[] tr_st = x_path.split("\\s+");
        
        // n is the length of the original path (in STATES, not transitions)
        int n = tr_st.length + 1;
        System.out.println("n (length of original path) = " + n);

        // Print original path with indices, for debugging
        // System.out.println("Original Path with Indices: ");
        // for (int tdx=0; tdx < tr_st.length; tdx++) {
        //   System.out.println(String.format("%d [%s]", tdx, tr_st[tdx]));
        // }

        // create a new path
        sim.createNewPath();
        sim.initialisePath(null);

        // set up the states for the state graph in a list for now
        // todo: currently assuming that there are no duplicate states
        // todo: currently assuming only one commuted transition

        // ArrayList<int[]> states = new ArrayList<int[]>();
        ArrayList<State> states = new ArrayList<State>();

        // Make an array of commuted states to cross-check the 
        // independent transition attribute
        ArrayList<State> commutedStates = new ArrayList<State>();      
        
        // set up the transitions for the state graph -- now done within states
        // ArrayList<Transition> transitions = new ArrayList<Transition>();

        // todo: make an arraylist of transitions
        // <[from to rate] ... >

        // set up a rolling state index to add new states
        int rollingStateIndex = 0;
        int transitionCount = 0;

        // Make the absorbing state
        // TODO: For now, we just hard-coded the absorbing state to match 
        // the six-reaction model. Eventually fix this with minimum value - 1.

        // states.add(new int[]{-1,-1,-1,-1,-1,-1});
        // NOT AN ABSORBING STATE. NOW, STATE 0 IS JUST A FILLER.
        states.add(new State(rollingStateIndex, new int[]{-5,-5,-5,-5,-5,-5,-5}, 0.0));
        
        // explicitly set index to 1 for initial state
        rollingStateIndex = 1;

        // Make the initial state (should be currentstate at this point)
        Object[] tl = sim.getCurrentState().varValues;
        int[] varv = new int[tl.length];
        for (int i = 0; i < tl.length; i++) {
          // Check if Object is an Integer or a String
          if (tl[i] instanceof Integer) {
            varv[i] = (Integer) tl[i];
          }
          else if (tl[i] instanceof String) {
            varv[i] = Integer.parseInt((String) tl[i]);
          }
        }
        // todo: don't hard code teh varv array in.
        // states.add(new int[]{varv[0],varv[1],varv[2],varv[3],varv[4],varv[5]});
        states.add(new State(rollingStateIndex, new int[]{varv[0],varv[1],varv[2],varv[3],varv[4],varv[5],varv[6]}, 0.0));
        commutedStates.add(new State(rollingStateIndex, new int[]{varv[0],varv[1],varv[2],varv[3],varv[4],varv[5],varv[6]}, 0.0));

        // TODO: Make it work for any array size.

        int index;
        // walk along the original path, getting probabilities as we go
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
          // System.out.println(String.format("CURRENT STATE A IS STATE %s", sim.getCurrentState()));
          // System.out.println(String.format("FIRED %s (%d)", sim.getTransitionActionString(index), index));
          // need to add state before manualTransition because the transition probability index 
          states.get(rollingStateIndex).addTransition(rollingStateIndex+n, sim.getTransitionProbability(index), index, sim.getTransitionActionString(index));
          transitionCount++;
          sim.manualTransition(index);
          // System.out.println(String.format("FIRED %s (%d)", sim.getTransitionActionString(index), index));
          // System.out.println(String.format("CURRENT STATE B IS STATE %s", sim.getCurrentState()));
          // Add t_alpha in a way that we can remove it later if needed
          // states.get(rollingStateIndex).addTransition(int to, double rate, int transitionIndex, String transitionName)
          // get variable values to build the state
          Object[] templist_c = sim.getCurrentState().varValues;
          int[] vval_c = new int[templist_c.length]; // vval_c for varValues
          for (int i = 0; i < templist_c.length; i++) {
            // Check if Object vval_c is an Integer or a String
            // System.out.println(templist_c[i].getClass().getName());
            // System.out.println(templist_c[i]);
            if (templist_c[i] instanceof Integer) {
              vval_c[i] = (Integer) templist_c[i];
            }
            else if (templist_c[i] instanceof String) {
              vval_c[i] = Integer.parseInt((String) templist_c[i]);
            }
            // vval_c[i] = Integer.valueOf((String) templist_c[i]);
          }
          commutedStates.add(new State(-2, new int[]{vval_c[0],vval_c[1],vval_c[2],vval_c[3],vval_c[4],vval_c[5],vval_c[6]}, 0.0));

          // go back to the original path
          // System.out.println(String.format("AFTER T_ALPHA IS STATE %s", sim.getCurrentState()));
          sim.backtrackTo(rollingStateIndex-1);
          // System.out.println(String.format("BACKTRACKED TO STATE %s", sim.getCurrentState()));
          

          index=0;
          // the loops are separate because we want to get the total probability.
          // we stop the next loop once we find our transition.
          for (int idx=0; idx < sim.getNumTransitions(); idx++) {
            String s1 = String.format("[%s]",tr_st[tdx]);
            String s2 = sim.getTransitionActionString(idx);
            if (s1.equalsIgnoreCase(s2)) {
                index = idx;
                break;
            }
          }
          // double transition_rate = sim.getTransitionProbability(index);
          // System.out.printf("sim.getTransitionProbability() = ");
          // System.out.println(sim.getTransitionProbability(index));
          // states.get(rollingStateIndex).addTransition(int to, double rate, int transitionIndex, String transitionName)
          
          // this needs to fire before manualTransition
          // todo: modularize this so much better oh my goodness
          states.get(rollingStateIndex).addTransition(rollingStateIndex+1, sim.getTransitionProbability(index), index, sim.getTransitionActionString(index));
          transitionCount++;
          // transitions.add(new Transition(rollingStateIndex,rollingStateIndex+1,transition_rate,index,sim.getTransitionActionString(index)));

          // System.out.println(String.format("CURRENT STATE C IS STATE %s", sim.getCurrentState()));
          // fire the transition
          sim.manualTransition(index);
          // System.out.println(String.format("FIRED %s (%d)", sim.getTransitionActionString(index), index));
          rollingStateIndex++;
          // System.out.println(String.format("CURRENT STATE D IS STATE %s", sim.getCurrentState()));
          
          // System.out.println(String.format("FIRST: State at tdx=%d, transition index=%d, transition name=%s", tdx, index, sim.getTransitionActionString(index)));
          // System.out.println("State Values afer firing transition");
          // System.out.println(sim.getCurrentState());

          // (found at parser->State.java, line 41");
          Object[] templist = sim.getCurrentState().varValues;
          int[] vv = new int[templist.length]; // vv for varValues
          for (int i = 0; i < templist.length; i++) {
            // Check if Object vv is an Integer or a String
            // System.out.println(templist[i].getClass().getName());
            // System.out.println(templist[i]);
            if (templist[i] instanceof Integer) {
              vv[i] = (Integer) templist[i];
            }
            else if (templist[i] instanceof String) {
              vv[i] = Integer.parseInt((String) templist[i]);
            }
            // vv[i] = Integer.valueOf((String) templist[i]);
          }
          states.add(new State(rollingStateIndex, new int[]{vv[0],vv[1],vv[2],vv[3],vv[4],vv[5],vv[6]}, 0.0));
          // System.out.println(Arrays.toString(vv));
        }

        // notes on backtrack to:
        // cannot be rollingStateIndex here (nothing index 20)
        // state 0 is initial state; so it's offset by -1 from rollingStateIndex.

        // test this backtrack
        // for (int i = rollingStateIndex-1; i >= 0; i--) {
        //   sim.backtrackTo(i);
        //   System.out.println(String.format("BacktrackTo %d yields state %s", i, sim.getCurrentState()));
        // }


        // sim.backtrackTo(2);
        // System.out.println(sim.getCurrentState());

        // start over and walk along the commuted path, doing the same thing.

        // SECOND LOOP -- COMMUTED PATH // SECOND LOOP -- COMMUTED PATH
        // SECOND LOOP -- COMMUTED PATH // SECOND LOOP -- COMMUTED PATH
        // SECOND LOOP -- COMMUTED PATH // SECOND LOOP -- COMMUTED PATH
        // SECOND LOOP -- COMMUTED PATH // SECOND LOOP -- COMMUTED PATH
        // SECOND LOOP -- COMMUTED PATH // SECOND LOOP -- COMMUTED PATH

        // create a new path on sim2, go back to initial state
        SimulatorEngine sim2 = prism.getSimulator();
        sim2.createNewPath();
        sim2.initialisePath(null);

        // fire the commutable transition from initial state to verify path correctness
        index = 0;
        for (int idx=0; idx < sim2.getNumTransitions(); idx++) {
          String s1 = String.format("[%s]",commute[0]);
          String s2 = sim2.getTransitionActionString(idx);
          if (s1.equalsIgnoreCase(s2)) {
              index = idx;
              break;
          }
        }
        // double transition_rate = sim2.getTransitionProbability(index);
        // System.out.printf("sim2.getTransitionProbability(commute[0].index) = ");
        // System.out.println(sim2.getTransitionProbability(index));
        // since it is from the initial state, go from 1 to rollingStateIndex+1 (which should be n+1)
        // transitions.add(new Transition(1,rollingStateIndex+1,transition_rate,index,sim2.getTransitionActionString(index)));
        
        // this needs to be before manualTransition
        // System.out.println("HERE");
        // states.get(1).addTransition(rollingStateIndex+1, sim2.getTransitionProbability(index), index, sim2.getTransitionActionString(index));
        // we already added t_alpha here. There's no need to add it again like the line above.
        // // fire the commuted transition
        // System.out.println(String.format("COM FIRED %s (%d)", sim.getTransitionActionString(index), index));
        sim2.manualTransition(index);
        rollingStateIndex++;

        // (found at parser->State.java, line 41");
        Object[] templist = sim2.getCurrentState().varValues;
        int[] vv = new int[templist.length]; // vv for varValues
        for (int i = 0; i < templist.length; i++) {
          // Check if Object vv is an Integer or a String
          // System.out.println(templist[i].getClass().getName());
          if (templist[i] instanceof Integer) {
            vv[i] = (Integer) templist[i];
          }
          else if (templist[i] instanceof String) {
            vv[i] = Integer.parseInt((String) templist[i]);
          }
          // vv[i] = Integer.valueOf((String) templist[i]);
        }
        // add the COMMUTED TRANSITION state (state n+1)
        states.add(new State(rollingStateIndex, new int[]{vv[0],vv[1],vv[2],vv[3],vv[4],vv[5],vv[6]}, 0.0));


        for (int tdx=0; tdx < tr_st.length; tdx++) { 
          index = 0;
          // get the total rate
          for (int idx=0; idx < sim2.getNumTransitions(); idx++) {
            states.get(rollingStateIndex).addRate(sim2.getTransitionProbability(idx));
            // System.out.println(String.format("Rate: %.6f at index %d",sim2.getTransitionProbability(idx),idx));
            // todo: save the total outgoing rate in a state object
            // todo: make states an object, not an int array.
          } 
          // the loops are separate because we want to get the total probability.
          // we stop the next loop once we find our transition.
          for (int idx=0; idx < sim2.getNumTransitions(); idx++) {
            String s1 = String.format("[%s]",tr_st[tdx]);
            String s2 = sim2.getTransitionActionString(idx);
            if (s1.equalsIgnoreCase(s2)) {
                index = idx;
                break;
            }
          }
          // transition_rate = sim2.getTransitionProbability(index);
          // System.out.printf("sim2.getTransitionProbability() = ");
          // System.out.println(sim2.getTransitionProbability(index));
          // transitions.add(new Transition(rollingStateIndex,rollingStateIndex+1,transition_rate,index,sim2.getTransitionActionString(index)));
          // needs to be before manualTransition
          states.get(rollingStateIndex).addTransition(rollingStateIndex+1, sim2.getTransitionProbability(index), index, sim2.getTransitionActionString(index));
          transitionCount++;

          // fire the transition
          // System.out.println(String.format("FIRED %s (%d)", sim.getTransitionActionString(index), index));
          sim2.manualTransition(index);
          rollingStateIndex++;
          
          // System.out.println(String.format("SECOND: State at tdx=%d, transition index=%d:", tdx, index));
          // System.out.println(sim2.getCurrentState());
          // System.out.println("State Values");
          // (found at parser->State.java, line 41");
          Object[] templist1 = sim2.getCurrentState().varValues;
          int[] vv1 = new int[templist1.length]; // vv1 for varValues
          for (int i = 0; i < templist1.length; i++) {
            // Check if Object vv1 is an Integer or a String
            // System.out.println(templist1[i].getClass().getName());
            // System.out.println(templist1[i]);
            if (templist1[i] instanceof Integer) {
              vv1[i] = (Integer) templist1[i];
            }
            else if (templist1[i] instanceof String) {
              vv1[i] = Integer.parseInt((String) templist1[i]);
            }
            // vv1[i] = Integer.valueOf((String) templist1[i]);
          }
          states.add(new State(rollingStateIndex, new int[]{vv1[0],vv1[1],vv1[2],vv1[3],vv1[4],vv1[5],vv1[6]}, 0.0));
          // System.out.println(Arrays.toString(vv1));
        }

        // check that t_alpha truly is independent
        boolean commutedCheck = true; // todo: false was to debug. set to true;

        for (int k = 1; k < commutedStates.size(); k++) {
          if (!states.get(n+k).equals(commutedStates.get(k))) {
            System.out.println("STATES NOT EQUAL. DETAILS ON NEXT LINE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
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

        rollingStateIndex++;

        int absorbIndex = rollingStateIndex;
        states.add(new State(absorbIndex, new int[]{-1,-1,-1,-1,-1,-1,-1}, 0.0));

        // get the absorbing state transition information
        System.out.println("Absorbing Index: " + absorbIndex);
        double absorbRate = 0.0;
        // don't self-loop to absorbing state
        for (int i = 1; i < states.size()-1; i++) {
          absorbRate = states.get(i).getAbsorbingRate();
          states.get(i).addTransition(absorbIndex, states.get(i).getAbsorbingRate(), -1, "ABSORB");
          transitionCount++;
          // int to, double rate, int transitionIndex, String transitionName
        }

        // write the sta and tra files in parallel to save time:

        String traStr = "";
        String staStr = "";
        String labStr = '0="init" 1="sink"\n0: 0\n';
        traStr += ((states.size()-1) + " " + transitionCount + "\n");
        staStr += "(";
        labStr += (absorbIndex + ": 1");
        String varName = "";
        int vari = 0;

        // get variable names for sta
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

        // get state and transition info by looping through states
        // state 0 is a filler state to make for easier math.
        // thus, start loop at index 1, not 0
        for (int i = 1; i < states.size(); i++) {
          for (int j = 0; j < states.get(i).outgoing.size(); j++) {
            // System.out.println(String.format("%2d -- %s", i, states.get(i).outgoing.get(j)));
            traStr += states.get(i).outgoing.get(j).prism();
            traStr += "\n";
          }
          staStr += states.get(i).prism();
          staStr += "\n";
        }

        BufferedWriter staWriter = new BufferedWriter(new FileWriter("model.sta"));
        staWriter.write(staStr);
        staWriter.close();

        BufferedWriter traWriter = new BufferedWriter(new FileWriter("model.tra"));
        traWriter.write(traStr);
        traWriter.close();

        BufferedWriter labWriter = new BufferedWriter(new FileWriter("model.lab"));
        labWriter.write(labStr);
        labWriter.close();

        // Print the states along the original path for debugging
        // System.out.println("States along Original Path");
        // for (int i = 0; i < states.size(); i++) {
        //   System.out.println(states.get(i));
        // }
        // System.out.println("Original Path States Complete.");

        // // Print the states along the commuted check path
        // System.out.println("States along Commuted Check Path");
        // for (int i = 0; i < commutedStates.size(); i++) {
        //   System.out.println(commutedStates.get(i));
        // }
        // System.out.println("Commuted Check Path States Complete.");

        // // Print the transitions along the original path
        // System.out.println("Transitions along Original Path");
        // for (int i = 0; i < states.size(); i++) {
        //   for (int j = 0; j < states.get(i).outgoing.size(); j++) {
        //     System.out.println(String.format("%2d -- %s", i, states.get(i).outgoing.get(j)));
        //   }
        // }
        System.out.println("Path Transitions Complete.");

        


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
          // totalRate = 0.0;
          for (int idx=0; idx < sim.getNumTransitions(); idx++) {
            // this line may have been causing issues -- I think I put it in the wrong loop
            // System.out.printf("tr %d: %s %f\n", idx, sim.getTransitionActionString(idx), sim.getTransitionProbability(idx));
            System.out.printf("%s ", sim.getTransitionActionString(idx).replace("[","").replace("]",""));
            // totalRate += sim.getTransitionProbability(idx);
          } // try combining these
          for (int idx=0; idx < sim.getNumTransitions(); idx++) {
            String s1 = String.format("[%s]",tr_st[tdx]);
            String s2 = sim.getTransitionActionString(idx);
            if (s1.equalsIgnoreCase(s2)) {
                index = idx;
                break;
            }
          }
          // double transition_probability = sim.getTransitionProbability(index) / totalRate;
          // System.out.printf("\n======= tr %s (%d) %e ===========\n\n", tr_st[tdx], index, transition_probability);
          System.out.printf("\n");
          // pathProbability *= transition_probability;
          // print what we fired before we fire it
          // System.out.println(String.format("FIRED %s (%d)", sim.getTransitionActionString(index), index));
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


// Maybe when we print we can just swap 0 and 1. That is, 0 is the initial state
// and 1 is the absorbing state. ???