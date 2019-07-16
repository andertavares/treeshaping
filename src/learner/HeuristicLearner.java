/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package learner;

import ai.core.AI;
import ai.core.ParameterSpecification;
import features.FeatureExtractor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import players.A3N;
import reward.RewardModel;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

/**
 *
 * @author artavares
 */
public class HeuristicLearner extends AI{
   /**
    * The weights are per heuristic
    */
   private Map<String, double[]> weights;

   /**
    * The vectors of eligibility traces (one per abstraction)
    */
   private Map<String, double[]> eligibility;

   private String previousChoiceName;

   private GameState previousState;
   
   private A3N planner;
   
   /**
    * The actual ID of this player
    */
   protected int playerID;

   /**
    * The learning rate for weight update
    */
   protected double alpha;

   /**
    * Probability of exploration
    */
   protected double epsilon;

   /**
    * The eligibility trace for weight update
    */
    protected double lambda;

   /**
    * The discount factor of future rewards
    */
   protected double gamma;

   /**
    * Time budget to return an action
    */
   protected int timeBudget;

   /**
    * The match duration
    */
   protected int matchDuration;

   /**
    * The random number generator
    */
   protected Random random;

   /**
    * The state feature extractor
    */
   protected FeatureExtractor featureExtractor;
	
    /**
     * This maps the AI name to its instance.
     * Each AI filters out the possible actions to consider at each state.
     * Thus they're called the action abstractions.
     */
    protected Map<String,AI> abstractions;
    
    /**
     * The reward model used by the agent
     */
    protected RewardModel rewards;
	
    protected Logger logger;
   
   public HeuristicLearner(UnitTypeTable types, Map<String,AI> portfolio, RewardModel rewards, FeatureExtractor featureExtractor, int matchDuration, int timeBudget, double alpha, 
			double epsilon, double gamma, double lambda, int randomSeed) 
    {
        this.timeBudget = timeBudget;
        this.alpha = alpha;
        this.epsilon = epsilon;
        this.gamma = gamma;
        this.lambda = lambda;
        this.rewards = rewards;
        this.matchDuration = matchDuration;

        random = new Random(randomSeed);

        this.featureExtractor = featureExtractor;

        // uses logistic with log loss by default
        //activation = new LogisticLogLoss();
		
    	logger = LogManager.getRootLogger();
		
        //initialize previous choice and state as null (they don't exist yet)
        previousChoiceName = null;
        previousState = null;

        // initialize weights and eligibility
        weights = new HashMap<>();
        eligibility = new HashMap<>();

        for (String aiName : abstractions.keySet()) {

            eligibility.put(aiName, new double[featureExtractor.getNumFeatures()]);

            // initializes weights randomly within [-1, 1]
            double[] abstractionWeights = new double[featureExtractor.getNumFeatures()];
            for (int i = 0; i < abstractionWeights.length; i++) {
                    abstractionWeights[i] = (random.nextDouble() * 2) - 1; // randomly initialized in [-1,1]
            }
            weights.put(aiName, abstractionWeights);

        }
    }

    @Override
    public void reset() {
        planner.reset();
    }

    @Override
    public PlayerAction getAction(int player, GameState gs) throws Exception {
        return planner.getAction(player, gs);
    }

    @Override
    public AI clone() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

   
   
}
