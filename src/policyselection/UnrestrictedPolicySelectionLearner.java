/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package policyselection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ai.core.AI;
import ai.core.ParameterSpecification;
import learning.LearningAgent;
import learning.LinearSarsaLambda;
import players.A3N;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

/**
 *
 * @author artavares
 */
public class UnrestrictedPolicySelectionLearner extends AI{
   
   /**
    * Interval between decision points
    */
   private int decisionInterval;
   
   
   /**
    * All choices (one per frame)
    */
   public List<String> choices;

   
   private A3N planner;
   
   /**
    * The actual ID of this player
    */
   protected int playerID;

   /**
    * Time budget to return an action
    */
   protected int timeBudget;

   /**
    * The match duration
    */
   protected int maxCycles;

   /**
    * The random number generator
    */
   protected Random random;

   String currentChoiceName;
    
   protected Logger logger;
   
   LearningAgent learner;

   /**
    * Creates an agent that attempts to learn the unrestricted unit selection policy
    * @param types
    * @param selectionStrategies
    * @param matchDuration
    * @param timeBudget
    * @param randomSeed
    */
   public UnrestrictedPolicySelectionLearner(
		   UnitTypeTable types,  LearningAgent learner,  
		   int maxCycles, int timeBudget, int decisionInterval) 
    {
	   	this.learner = learner;
	   	this.maxCycles = maxCycles;
        this.timeBudget = timeBudget;
        this.decisionInterval = decisionInterval;

        planner = new A3N(types);
        choices = new ArrayList<>();
    	logger = LogManager.getRootLogger();
    }
   
   /**
    * Instantiates a policy selector with parameters from a config object
    * @param types
    * @param randomSeed
    * @param config
    */
   public UnrestrictedPolicySelectionLearner(UnitTypeTable types, int randomSeed, Properties config) {
	   this(
		   types, 
		   new LinearSarsaLambda(types, config, randomSeed), //TODO allow the retrieval of other learning agents
		   Integer.parseInt(config.getProperty("max_cycles")),
		   Integer.parseInt(config.getProperty("search.timebudget")),
		   Integer.parseInt(config.getProperty("decision_interval"))
	   );
   }
    
   /**
    * Returns the choices performed by this agent
    * @return
    */
   public List<String> getChoices(){
	   return choices;
   }

	@Override
    public void reset() {
        planner.reset();
    }

    @Override
    public PlayerAction getAction(int player, GameState gs) throws Exception {
    	//sanity check for player ID:
		if(gs.getTime() == 0) {
			playerID = player; //assigns the ID on the initial state
			
		} else if (player != playerID) { // consistency check for other states
			logger.error("Called to play with different ID! (mine={}, given={}", playerID, player);
			logger.error("Will proceed, but behavior might be unpredictable");
		}
		
		// performs a new choice if the interval has passed
		if (decisionInterval <= 1 || gs.getTime() % decisionInterval == 0) { 
			// determines the unrestricted selection policy
			currentChoiceName = learner.act(gs, player);
			
		}

		// logs and stores the current choice (even if unchanged)
		logger.debug("Frame {}. Player {} chose: {}.", gs.getTime(), player, currentChoiceName);
		choices.add(currentChoiceName);
		
		// sets the unrestricted unit selection policy
    	planner.setUnrestrictedSelectionPolicy(currentChoiceName, 1);
    	
    	// gets the action returned by the planner according to the unrestricted selection policy
        return planner.getAction(player, gs);
    }
    
    @Override
	public void gameOver(int winner) {
		/*
		 *  if learning from actual experience, the agent never is called to act
		 *  in a terminal state and therefore, never sees the final reward, 
		 *  which is the most important
		 */
		logger.debug("gameOver. winner={}, playerID={}", winner, playerID);
		learner.finish(winner);
	}

	@Override
    public AI clone() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
	 * Saves learner's weights to a binary file
	 * 
	 * @param path
	 * @throws IOException
	 */
	public void saveWeights(String path) throws IOException {
		learner.save(path);
	}

	/**
	 * Load weights from a binary file
	 * 
	 * @param path
	 * @throws IOException
	 */
	public void loadWeights(String path) throws IOException {
		learner.load(path);
	}

   
   
}
