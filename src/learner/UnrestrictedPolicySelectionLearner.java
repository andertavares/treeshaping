/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package learner;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ai.core.AI;
import ai.core.ParameterSpecification;
import features.FeatureExtractor;
import features.FeatureExtractorFactory;
import players.A3N;
import portfolio.PortfolioManager;
import reward.RewardModel;
import reward.RewardModelFactory;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

/**
 *
 * @author artavares
 */
public class UnrestrictedPolicySelectionLearner extends AI{
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
    protected Map<String,AI> policies;
    
    /**
     * The reward model used by the agent
     */
    protected RewardModel rewards;
    
    /**
     * A map from acronyms to actual class names inside A3N original project
     */
    @SuppressWarnings("serial")
	protected final static Map<String,String> selectionStrategyNames = new HashMap<>() {{
    	put("CC", "ManagerClosest");
    	put("CE", "ManagerClosestEnemy");
    	put("FC", "ManagerFather");
    	put("FE", "ManagerFartherEnemy");
    	put("AV-", "ManagerLessDPS");
    	put("AV+", "ManagerMoreDPS");
    	put("HP-", "ManagerLessLife");
    	put("HP+", "ManagerMoreLife");
    	put("R", "ManagerRandom");
    	put("M", "ManagerUnitsMelee");
    }};
    
   protected Logger logger;
   
   /**
    * Creates an agent that attemps to learn the unrestricted unit selection policy
    * TODO get rid of portfolio?
    * @param types
    * @param portfolio
    * @param rewards
    * @param featureExtractor
    * @param selectionStrategyAcronyms
    * @param matchDuration
    * @param timeBudget
    * @param alpha
    * @param epsilon
    * @param gamma
    * @param lambda
    * @param randomSeed
    */
   public UnrestrictedPolicySelectionLearner(
		   UnitTypeTable types, Map<String,AI> portfolio, RewardModel rewards, 
		   FeatureExtractor featureExtractor, List<String> selectionStrategyAcronyms, 
		   int matchDuration, int timeBudget, double alpha, 
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

        for (String strAcronym : selectionStrategyAcronyms) {

        	String strategyName = selectionStrategyNames.get(strAcronym.trim());
            eligibility.put(strategyName, new double[featureExtractor.getNumFeatures()]);

            // FIXME: in tdLambdaUpdateRule, w has 24 positions and e has 16!
            // initializes weights randomly within [-1, 1]
            double[] strategyWeights = new double[featureExtractor.getNumFeatures()];
            for (int i = 0; i < strategyWeights.length; i++) {
                    strategyWeights[i] = (random.nextDouble() * 2) - 1; // randomly initialized in [-1,1]
            }
            weights.put(strategyName, strategyWeights);

        }
        
        //instantiates the A3N planner
        planner = new A3N(types);
    }
   
   public static UnrestrictedPolicySelectionLearner fromConfig(UnitTypeTable types, int randomSeed, Properties config) {
	   
	   Logger logger = LogManager.getRootLogger();
	   
	   int maxCycles = Integer.parseInt(config.getProperty("max_cycles"));
		
	   int timeBudget = Integer.parseInt(config.getProperty("search.timebudget"));
		
       double epsilon = Double.parseDouble(config.getProperty("td.epsilon.initial"));
       //epsilonDecayRate = Double.parseDouble(config.getProperty("td.epsilon.decay", "1.0"));
       
       double alpha = Double.parseDouble(config.getProperty("td.alpha.initial"));
       //alphaDecayRate = Double.parseDouble(config.getProperty("td.alpha.decay", "1.0"));
       
       double gamma = Double.parseDouble(config.getProperty("td.gamma"));
       double lambda = Double.parseDouble(config.getProperty("td.lambda"));
       
       String portfolioNames = config.getProperty("portfolio");
	   
       // loads the reward model (default=winlossdraw)
       RewardModel rewards = RewardModelFactory.getRewardModel(
		   config.getProperty("rewards", "winlossdraw"), maxCycles
	   );
       logger.debug("Reward model: {}", rewards.getClass().getSimpleName());
       
       FeatureExtractor featureExtractor = FeatureExtractorFactory.getFeatureExtractor(
		   config.getProperty("features", "materialdistancehp"), types, maxCycles
	   );
       
       List<String> selectionStrategies = Arrays.asList(
		   config.getProperty("strategies", "CE,FE,HP-,HP+,AV+").split(",")
	   );
       
       // returns a new instance with the loaded parameters
       return new UnrestrictedPolicySelectionLearner(
			types, 
			PortfolioManager.getPortfolio(types, Arrays.asList(portfolioNames.split(","))), 
			rewards,
			featureExtractor,
			selectionStrategies,
			maxCycles,
			timeBudget, alpha, epsilon, gamma, lambda, randomSeed
		);
	   
   }
    
   /**
    * Returns the weight vector
    * @return
    */
   public Map<String, double[]> getWeights(){
	   return weights;
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
    	
		// determines the unrestricted selection policy
		String currentChoiceName = epsilonGreedy(gs, player, weights, epsilon);
		
		if(previousState != null && previousChoiceName != null) {
			// learns from actual experience
			sarsaUpdate(previousState, player, previousChoiceName, gs, currentChoiceName, weights, eligibility);
		}
		
		// updates previous choices for the next sarsa learning update
		previousChoiceName = currentChoiceName;
		previousState = gs.clone(); //cloning fixes a subtle error where gs changes in the game engine and becomes the next state, which is undesired 
		
		//Date end = new Date(System.currentTimeMillis());
		logger.debug("Player {} selected {}.",
			player, currentChoiceName
		);
		

		// sets the unrestricted unit selection policy
    	planner.setUnrestrictedSelectionPolicy(currentChoiceName, 1);
    	
    	// gets the action returned by the planner according to the unrestricted selection policy
        return planner.getAction(player, gs);
    }

    /**
	 * Performs a Sarsa update on the given weights using the given eligibility traces.
	 * For an experience tuple <s, a, r, s', a'>, where s is the state, a is the actionName, 
	 * r is the reward (calculated internally),
	 * s' is the next state, a' is the nextActionName
	 * 
	 * 1) Calculates the TD error: delta = r + gammna * Q(s',a') - Q(s,a) 
	 * 2) Updates the weight vector: w = w + alpha * delta * e (e is the eligibility vector) 
	 * 3) Updates the eligibility vector: e = lambda * gamma * e + features
	 * @param state
	 * @param player
	 * @param actionName
	 * @param nextState
	 * @param nextActionName
	 * @param weights
	 * @param eligibility
	 */
	private void sarsaUpdate(GameState state, int player, String actionName, GameState nextState, String nextActionName, 
			Map<String, double[]> weights, Map<String, double[]> eligibility) {
		
		logger.debug(
			"<s,a,r,s'(gameover?),a',q(s',a')> = <{}, {}, {}, {}({}), {}, {}>",
			state.getTime(), actionName, 
			rewards.reward(nextState, player), 
			nextState.getTime(), nextState.gameover(), nextActionName,
			qValue(nextState, player, nextActionName)
		);
		
		//delta = r + gammna * Q(s',a') - Q(s,a)
		double tdError = tdTarget(nextState, player, nextActionName) - qValue(state, player, actionName);

		tdLambdaUpdateRule(state, player, actionName, tdError, weights, eligibility);
		
		/*
		 * Remark: in Silver et al (2013) TD search, the eligibility vector update is done as 
		 * e = e * lambda + f(s,a), where f(s,a) are the features for state s and action a.
		 * This is so because features are per state and action. 
		 * Moreover, they use gamma=1 always so that it does not appear in the equation.
		 * That is, the general form of the equation should be e = e * gamma * lambda + f(s,a)
		 *  
		 * Here, gamma can have different values and we can interpret that f(s,a) = zeros 
		 * for the non-selected action.
		 * Thus, we decay the eligibility vectors of all actions and then 
		 * increase the eligibility vector of the selected action by adding the current state features.
		 * In other words, we  implement equation e = e * gamma * lambda + f(s,a) in two steps.
		 */
	}

	private void tdLambdaUpdateRule(GameState state, int player, String actionName, double tdError, Map<String, double[]> weights,
			Map<String, double[]> eligibility) {
		
		double[] f = featureExtractor.extractFeatures(state, player); // feature vector for the state
		
		for (String strategyName : weights.keySet()) {
			
			double[] w = weights.get(strategyName); // weight vector
			double[] e = eligibility.get(strategyName); // eligibility vector 
			
			// certifies that things are ok
			assert w.length == e.length;
			assert e.length == f.length;
			
			// vector updates FIXME error when strategy names are not present on the weight vector
			for (int i = 0; i < w.length; i++) {
				w[i] = w[i] + alpha * tdError * e[i]; // weight vector update
				e[i] = e[i] * gamma * lambda; //the eligibility of all actions decays by gamma * lambda
			}
		}
		
		// incrementes the eligibility of the selected action by adding the feature vector
		double[] eSelected = eligibility.get(actionName);
		for (int i = 0; i < eSelected.length; i++) {
			eSelected[i] += f[i];
		}
	}

	/**
	 * The temporal-difference target is, by definition, r + gamma * q(s', a'),
	 * where s' is the reached state and a' is the action to be performed there.
	 * 
	 * @param nextState
	 * @param player
	 * @param nextActionName
	 * @return
	 */
	private double tdTarget(GameState nextState, int player, String nextActionName) {
		double reward, nextQ;
		reward = rewards.reward(nextState, player);
		
		// terminal states have value of zero
		if (nextState.gameover() || nextState.getTime() >= matchDuration) {
			nextQ = 0;
		} else {
			nextQ = qValue(nextState, player, nextActionName);
		}
		logger.trace("Reward for time {} for player {}: {}. q(s',a')={}. GameOver? {}", nextState.getTime(), player, reward, nextQ, nextState.gameover());
		return reward + this.gamma * nextQ;
	}

	/**
	 * Returns the name of the unrestricted unit selection policy that would be 
	 * active in this state using epsilon greedy
	 * (a random strategy name with probability epsilon, and the greedy w.r.t the Q-value
	 * with probability (1-epsilon)
	 * 
	 * @param state
	 * @param player
	 * @param weights
	 * @param epsilon 
	 * @return
	 */
    private String epsilonGreedy(GameState state, int player, Map<String, double[]> weights, double epsilon) {
    	// the name of the AI that will choose the action for this state
		String chosenName = null;

		// epsilon-greedy:
		if (random.nextDouble() < epsilon) { // random choice
			// trick to randomly select from HashMap adapted from:
			// https://stackoverflow.com/a/9919827/1251716
			List<String> keys = new ArrayList<String>(weights.keySet());
			chosenName = keys.get(random.nextInt(keys.size()));
			if (chosenName == null) {
				logger.error("Unable to select a random strategy!");
			}
		} else { // greedy choice
			chosenName = greedyChoice(state, player, weights);
		}

		return chosenName;
	}

    /**
	 * Returns the name of the unrestricted unit selection policy with the highest Q-value for
	 * the given state
	 * 
	 * @param state
	 * @param player
	 * @return
	 */
	private String greedyChoice(GameState state, int player, Map<String, double[]> weights) {

		// the name of the strategy that be active for this state
		String chosenName = null;

		// feature vector
		double[] features = featureExtractor.extractFeatures(state, player);

		// argmax Q:
		double maxQ = Double.NEGATIVE_INFINITY; // because MIN_VALUE is positive =/
		for (String candidateName : weights.keySet()) {
			double q = qValue(features, candidateName);
			if(Double.isInfinite(q) || Double.isNaN(q)) {
				logger.warn("(+ or -) infinite qValue for action {} in state {}", candidateName, features); 
			}
			if (q > maxQ) {
				maxQ = q;
				chosenName = candidateName;
			}
		}
		if (chosenName == null) {
			logger.error("Unable to select a strategy for the greedy action in state {}! Selecting ManagerClosestEnemy to avoid a crash.", state.getTime());
			logger.error("Dumping state to errorState{}.xml", state.getTime());
			state.toxml("errorState" + state.getTime() + ".xml");
			//chosenName = "ManagerClosestEnemy";
		}

		return chosenName;
	}

	/**
	 * Returns the Q-value of the unrestricted unit selection policy for the state described by the
	 * given feature vector
	 * 
	 * @param features
	 * @param policyName
	 * @return
	 */
	private double qValue(double[] features, String policyName) {
		return dotProduct(features, weights.get(policyName));
	}
	
	/**
	 * Returns the Q-value for the given state-action pair
	 * 
	 * @param state
	 * @param player
	 * @param actionName the unrestricted unit selection policy
	 * @return
	 */
	private double qValue(GameState state, int player, String actionName) {
		return qValue(featureExtractor.extractFeatures(state, player), actionName);
	}

	/**
	 * Dot product between two vectors (i.e. sum(f[i] * w[i]) for i = 0, ..., length of the vectors
	 * @param features
	 * @param weights
	 * @return
	 */
	protected double dotProduct(double[] features, double[] weights) {
		assert features.length == weights.length;
		
		double value = 0;
		for(int i = 0; i < features.length; i++) {
			value += features[i] * weights[i];
		}
		return value;
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
	 * Saves weights to a binary file
	 * 
	 * @param path
	 * @throws IOException
	 */
	public void saveWeights(String path) throws IOException {
		FileOutputStream fos = new FileOutputStream(path);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(weights);
		oos.close();
		fos.close();
	}

	/**
	 * Load weights from a binary file
	 * 
	 * @param path
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void loadWeights(String path) throws IOException {
		FileInputStream fis = new FileInputStream(path);
		ObjectInputStream ois = new ObjectInputStream(fis);
		try {
			weights = (Map<String, double[]>) ois.readObject();
		} catch (ClassNotFoundException e) {
			System.err.println("Error while attempting to load weights.");
			e.printStackTrace();
		}
		ois.close();
		fis.close();
	}

   
   
}
