import java.io.File;
import java.io.FileNotFoundException;

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

      // create a new path
      // sim.createNewPath();
      sim.createNewOnTheFlyPath(); // recommended for efficiency
      sim.initialisePath(null);

      // for now, do 3 random steps
      sim.automaticTransition();
			sim.automaticTransition();
			sim.automaticTransition();
			System.out.println("A random path (3 steps):");
			System.out.println(sim.getPath());
      
      // follow the transitions
      // TODO: read in the transitions and follow them

      // close PRISM
      prism.closeDown();
    } 
    catch (FileNotFoundException e) {
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		} 
    catch (PrismException e) {
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
  }
}