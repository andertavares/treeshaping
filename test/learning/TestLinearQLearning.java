package learning;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jdom.JDOMException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rts.GameState;
import rts.PhysicalGameState;
import rts.units.Unit;
import rts.units.UnitTypeTable;
import utils.StrategyNames;

class TestLinearQLearning {

	LinearQLearning learner;
	MockupFeatureExtractor testFeatureExtractor;
	MockupRewardModel testRewardModel;
	UnitTypeTable types;
	double alpha, gamma, lambda;
	
	@BeforeEach
	void setUp() throws Exception {
		types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
		testFeatureExtractor = new MockupFeatureExtractor(new double[] {1.0, 0.5});
		testRewardModel = new MockupRewardModel(0.1, 0);
		alpha = 0.01;
		gamma = 0.9;
		
		learner = new LinearQLearning(
			types, 
			testRewardModel, 
			testFeatureExtractor, 
			StrategyNames.acronymsToNames(Arrays.asList(new String[] {"HP-","CE", "FC", "R"})), 
			alpha, 0.1, gamma, 0
		);
	}
	
	@Test
	/**
	 * LinearSarsaLambda raised an exception when instantiated from config without random seed
	 */
	void testCreationFromConfigWithoutRandomSeed() {
		Properties config = new Properties();
		config.put("max_cycles", "200");
		config.put("rewards", "winlossdraw");
		config.put("features", "materialdistancehp");
		config.put("strategies", "HP-,CE,FC,R"); 
		config.put("td.alpha.initial", "0.15");
		config.put("td.epsilon.initial", "0.1");
		config.put("td.gamma", "0.99");
		learner = new LinearQLearning(types, config);
		// if code reaches here without throwing an exception, we're good to go
	}
	
	@Test
	void testQValue() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		@SuppressWarnings("serial")
		Map<String, double[]> testWeights = new HashMap<>() {{
			put("action1", new double[] {0.3, 0.1});
		}};
		
		setLearnerWeights(testWeights);
		
		//feature vector is [1.0, 0.5], weights for 'action1' are: [0.3, 0.1], expected Q-value is: 0.3 + 0.05 = 0.35
		assertEquals(0.35, learner.qValue(new double[] {1.0, 0.5}, "action1"));
		
	}
	
	@Test
	void testGreedyChoice() throws NoSuchFieldException, IllegalAccessException {
		// creates two 'foo' game states that will be mapped to different feature vectors
		GameState s0 = new GameState(new PhysicalGameState(0, 0), types);
		GameState s1 = new GameState(new PhysicalGameState(3, 3), types);
		
		// adds the game states to one-hot feature encoding
		testFeatureExtractor.putMapping(s0, new double[] {1, 0});
		testFeatureExtractor.putMapping(s1, new double[] {0, 1});
		
		// custom set of weights: prefer action2 in s0 and action1 in s1
		@SuppressWarnings("serial")
		Map<String, double[]> testWeights = new HashMap<>() {{
			put("action1", new double[] {1, 2});
			put("action2", new double[] {4, -1});
		}};
		setLearnerWeights(testWeights);
		
		assertEquals("action2", learner.greedyChoice(s0, 0, testWeights));
		assertEquals("action1", learner.greedyChoice(s1, 0, testWeights));
	}
	
	@Test
	void testEpsilonGreedy() throws NoSuchFieldException, IllegalAccessException {
		// creates a 'foo' game state that will be mapped to different feature vectors
		GameState s0 = new GameState(new PhysicalGameState(0, 0), types);
		
		// adds the game states to one-hot feature encoding
		testFeatureExtractor.putMapping(s0, new double[] {1, 0});
		
		// custom set of weights: prefer action2 in s0
		@SuppressWarnings("serial")
		Map<String, double[]> testWeights = new HashMap<>() {{
			put("action1", new double[] {1, 2});
			put("action2", new double[] {4, -1});
		}};
		setLearnerWeights(testWeights);
		
		// counts the number of times that action2 was chosen in a 1M selections:
		int greedyChoice = 0;
		for(int i = 0; i < 1000000; i++) {
			if(learner.epsilonGreedy(s0, 0).equals("action2")) {
				greedyChoice++;
			}
		}
		
		// on the random choice, it is expected that the greedy action is also taken 50% of the times
		// in total, the greedy action is expected to be taken 90% + (50% * 10%) = 95%
		
		//with a tolerance of 200 choices, checks that the greedy action was taken 95% of the time
		assertEquals(950000.0, greedyChoice, 200); 
	}
	
	
	
	@Test 
	void testQLearningUpdate() throws JDOMException, IOException, Exception {
		
		// loads a physical game state that is not a game over
		PhysicalGameState pgs = PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", types);
		
		// creates three 'foo' game states that will be mapped to different feature vectors
		// they are different in the number of units (microRTS does not differentiates states based solely on time)
		GameState s0 = new GameState(pgs, types);
		GameState s1 = s0.clone(); s1.getPhysicalGameState().addUnit(new Unit(0, types.getUnitTypes().get(0), 3, 3)); 
		GameState s2 = s1.clone(); s2.getPhysicalGameState().addUnit(new Unit(0, types.getUnitTypes().get(0), 5, 5));  
		
		// encodes the game states with one-hot encoding
		double[][] features = new double[][] {
			{1, 0, 0}, 
			{0, 1, 0},
			{0, 0, 1}
		};
		testFeatureExtractor.putMapping(s0, features[0] );
		testFeatureExtractor.putMapping(s1, features[1] );
		testFeatureExtractor.putMapping(s2, features[2] );
		
		// creates test weights
		@SuppressWarnings("serial")
		Map<String, double[]> testWeights = new HashMap<>() {{
			put("action1", new double[] {1, 2, 3});
			put("action2", new double[] {4, -1, -2});
		}};
		setLearnerWeights(testWeights);
		
		// sarsa tuple: s0, action2, +10, s1
		testRewardModel.setValues(10, 0);
		learner.learn(s0, 0, "action2", 10, s1, false);
		
		// tests q-values of the two actions
		assertArrayEquals(
			new double[] {1, 2, 3},  //action1 is unchanged
			new double[] {
				learner.qValue(features[0], "action1"), 
				learner.qValue(features[1], "action1"),
				learner.qValue(features[2], "action1"),
			}
		);
		
		double q_s0_a2 = 4 + alpha*(10 + gamma*4 - 4); //oldQ + alpha*(r + gamma*maxq(s1) - oldQ)
		assertArrayEquals(
			new double[] {q_s0_a2, -1, -2},  //action2 is changed on s0
			new double[] {
				learner.qValue(features[0], "action2"), 
				learner.qValue(features[1], "action2"),
				learner.qValue(features[2], "action2"),
			}
		);
		
		// next sars tuple: s1, action1, -100, s2 
		testRewardModel.setValues(-100, 0);
		learner.learn(s1, 0, "action1", -100, s2, false);
		
		// tests q-values of the two actions
		double tdError = -100 + gamma*2 - 2;
		assertArrayEquals( //perhaps break this into 3 assertEquals?
			new double[] {1, 2 + alpha*(tdError), 3},  //action1 changes on s1 as oldQ + alpha*(r + gamma*maxq(s1) - oldQ)
			new double[] {
				learner.qValue(features[0], "action1"), 
				learner.qValue(features[1], "action1"),
				learner.qValue(features[2], "action1"),
			}
		);
		
		q_s0_a2 = q_s0_a2 + alpha*tdError*gamma*lambda;
		assertArrayEquals(
			new double[] {q_s0_a2, -1, -2},  //action2 is changed on s0
			new double[] {
				learner.qValue(features[0], "action2"), 
				learner.qValue(features[1], "action2"),
				learner.qValue(features[2], "action2"),
			}
		);
		
	}
	
	@Test
	void testGameOver() throws Exception {
		// for this test, we'll use a learner with epsilon = 0, to control which action it would return
		learner = new LinearQLearning(
			types, 
			testRewardModel, 
			testFeatureExtractor, 
			StrategyNames.acronymsToNames(Arrays.asList(new String[] {"HP-","CE", "FC", "R"})), 
			alpha, 0.0, gamma, 0
		);
		
		// loads a physical game state that is not a game over
		PhysicalGameState pgs = PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", types);
		
		// creates two 'foo' game states that will be mapped to different feature vectors
		GameState s0 = new GameState(pgs, types);
		GameState s1 = new GameState(new PhysicalGameState(0, 0), types); //this is a terminal state
		
		// encodes the game states with one-hot encoding
		double[][] features = new double[][] {
			{1, 0}, 
			{0, 1},
		};
		testFeatureExtractor.putMapping(s0, features[0]);
		testFeatureExtractor.putMapping(s1, features[1]);
		
		// creates test weights
		@SuppressWarnings("serial")
		Map<String, double[]> testWeights = new HashMap<>() {{
			put("action1", new double[] {5, 2});
			put("action2", new double[] {4, -1});
		}};
		setLearnerWeights(testWeights);
		
		//agent should take action1 in s0
		String actionInS0 = learner.act(s0, 0);
		assertEquals("action1", actionInS0);
		
		// now let's do a gameOverUpdate as if the player has won, setting the game over reward to 1
		testRewardModel.setValues(0, 1);
		learner.finish(0); //winner is player 0 (our player under test) 
		
		double tdError = 1 - 5; //reward - q(s0,a1)
		// tests q-values: q = q + alpha * tderror * f)
		assertEquals(5 + alpha * tdError * 1, learner.qValue(features[0], "action1"));
		assertEquals(2, learner.qValue(features[1], "action1"));
		
		assertEquals(4, learner.qValue(features[0], "action2"));
		assertEquals(-1, learner.qValue(features[1], "action2"));
	}

	@Test
	void testLoadAndSaveWeights() throws NoSuchFieldException, IllegalAccessException, IOException {
		Map<String, double[]> testWeights = new HashMap<>(); 
		testWeights.put("action1", new double[] {0.1, 2});
		testWeights.put("action2", new double[] {4, -1});
		setLearnerWeights(testWeights);
		
		learner.save("testweights.bin");
		
		// changes some of the weights and verifies the change
		testWeights.get("action1")[0] = -1000;
		assertEquals(-1000, learner.qValue(new double[] {1, 0} , "action1"));
		
		// loads the previously saved weights
		learner.load("testweights.bin");
		
		// checks that the weights have their original values via qValue
		assertEquals(0.1, learner.qValue(new double[] {1, 0} , "action1"));
		assertEquals(2, learner.qValue(new double[] {0, 1} , "action1"));
		assertEquals(4, learner.qValue(new double[] {1, 0} , "action2"));
		assertEquals(-1, learner.qValue(new double[] {0, 1} , "action2"));
		
	}
	
	/**
	 * Sets the weights of our learner object
	 * @param weights
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 */
	private void setLearnerWeights(Map<String, double[]> weights) throws NoSuchFieldException, IllegalAccessException {
		
		// opens the visibility of weights, sets them and tests the Q-value
		Field field = LinearQLearning.class.getDeclaredField("weights");
		field.setAccessible(true);
		field.set(learner, weights);
		assertEquals(learner.getWeights(), weights);
	}
	
}
