/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package learner;

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
    * TODO get rid of portfolio?
    * @param types
    * @param portfolio
    * @param selectionStrategyAcronyms
    * @param matchDuration
    * @param timeBudget
    * @param randomSeed
    *
   public UnrestrictedPolicySelectionLearner(
		   UnitTypeTable types, Map<String,AI> portfolio, RewardModel rewards, 
		   FeatureExtractor featureExtractor, List<String> selectionStrategyAcronyms, 
		   int matchDuration, int timeBudget, int decisionInterval, int randomSeed) 
    {
        this.timeBudget = timeBudget;
        this.decisionInterval = decisionInterval;

        random = new Random(randomSeed);
        
        choices = new ArrayList<>();

        // uses logistic with log loss by default
        //activation = new LogisticLogLoss();
		
    	logger = LogManager.getRootLogger();
		
        //instantiates the A3N planner
        planner = new A3N(types);
        
    }*/
   
   public UnrestrictedPolicySelectionLearner(UnitTypeTable types, int randomSeed, Properties config) {
	   
	   maxCycles = Integer.parseInt(config.getProperty("max_cycles"));
	   timeBudget = Integer.parseInt(config.getProperty("search.timebudget"));
	   decisionInterval = Integer.parseInt(config.getProperty("decision_interval"));
	   learner = new LinearSarsaLambda(types, config, randomSeed);
	   choices = new ArrayList<>();

   	   logger = LogManager.getRootLogger();
		
       //instantiates the A3N planner
       planner = new A3N(types);
	   
	   currentChoiceName = null;
	   
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
		 *
		logger.debug("gameOver. winner={}, playerID={}", winner, playerID);
		double tdError = rewards.gameOverReward(playerID, winner) - qValue(previousState, playerID, previousChoiceName);
		
		tdLambdaUpdateRule(previousState, playerID, previousChoiceName, tdError, weights, eligibility);
		*/
		
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
