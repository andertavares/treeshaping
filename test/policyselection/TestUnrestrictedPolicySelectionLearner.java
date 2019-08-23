package policyselection;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import learning.LinearSarsaLambda;
import learning.MockupFeatureExtractor;
import learning.MockupRewardModel;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

class TestUnrestrictedPolicySelectionLearner {

	UnrestrictedPolicySelectionLearner policySelector;
	UnitTypeTable types;
	MockupFeatureExtractor testFeatureExtractor;
	MockupRewardModel testRewardModel;
	LinearSarsaLambda learner;
	
	double alpha, gamma, lambda;
	
	@BeforeEach
	void setUp() throws Exception {
		types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
		testFeatureExtractor = new MockupFeatureExtractor(new double[] {1.0, 0.5});
		testRewardModel = new MockupRewardModel(0.1, 0);
		alpha = 0.01;
		gamma = 0.9;
		lambda = 0.1;
		
		learner = new LinearSarsaLambda(
			types, 
			testRewardModel, 
			testFeatureExtractor, 
			Arrays.asList(new String[] {"action1","action2"}), 
			alpha, 0.1, gamma, lambda, 0
		);
		
		policySelector = new UnrestrictedPolicySelectionLearner(
			types, learner, 3000, 100, 1
		);
	}
	
	@Test
	void testGetAction() throws Exception {
		// for this test, creates a learner with no exploration rate
		learner = new LinearSarsaLambda(
			types, 
			testRewardModel, 
			testFeatureExtractor, 
			Arrays.asList(new String[] {"action1","action2"}), 
			alpha, 0, //epsilon is 0 
			gamma, lambda, 0
		);
		
		policySelector = new UnrestrictedPolicySelectionLearner(
			types, learner, 3000, 100, 1
		);
		
		
		
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
		
		
		
		// the policy selector is supposed to select action2. 
		// It will be on the 1st position of the choices list
		policySelector.getAction(0, s0);
		assertEquals(1, policySelector.getChoices().size());
		assertEquals("action2", policySelector.getChoices().get(0));
		
		// now, let's verify if the action took action1 in s1
		// it will be in 2nd position of the choices list
		policySelector.getAction(0, s1);
		assertEquals(2, policySelector.getChoices().size());
		assertEquals("action1", policySelector.getChoices().get(1));
		
	}
	
	void testDecisionInterval() {
		fail();
	}
	
	@Test
	/**
	 * This test verifies that the learner INSIDE the UnrestrictedPolicySelector has changed properly
	 * after a gameOver
	 * @throws Exception
	 */
	void testGameOver() throws Exception {
		// loads a physical game state that is not a game over
		PhysicalGameState pgs = PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", types);
		
		// creates three 'foo' game states that will be mapped to different feature vectors
		GameState s0 = new GameState(pgs, types);
		GameState s1 = new GameState(new PhysicalGameState(0, 0), types); //this is a terminal state
		
		// encodes the game states with one-hot encoding
		double[][] features = new double[][] {
			{1, 0}, 
			{0, 1},
		};
		testFeatureExtractor.putMapping(s0, features[0] );
		testFeatureExtractor.putMapping(s1, features[1] );
		
		// creates test weights
		@SuppressWarnings("serial")
		Map<String, double[]> testWeights = new HashMap<>() {{
			put("action1", new double[] {0.1, 2});
			put("action2", new double[] {4, -1});
		}};
		setLearnerWeights(testWeights);
		
		//let's simulate that the agent took action1 in s0 
		Field prevStateField = learner.getClass().getDeclaredField("nextState");
		prevStateField.setAccessible(true);
		prevStateField.set(learner, s0);
		
		Field actionField = learner.getClass().getDeclaredField("nextAction");
		actionField.setAccessible(true);
		actionField.set(learner, "action1");
		
		// let's also simulate that the agent has the following eligibility traces:
		@SuppressWarnings("serial")
		Map<String, double[]> eligibility = new HashMap<>() {{
			put("action1", new double[] {0.03, 0.1});
			put("action2", new double[] {0.2, 0});
		}};
		Field eligField = learner.getClass().getDeclaredField("eligibility");
		eligField.setAccessible(true);
		eligField.set(learner, eligibility);
		
		// now let's do an gameOverUpdate as if the player has won, setting the game over reward as 1
		testRewardModel.setValues(0, 1);
		policySelector.gameOver(0); 
		
		// tests eligibility of the two actions (increased+decayed for a1, decayed for a2)
		assertArrayEquals(
			new double[] {(0.03+1) * gamma*lambda, 0.1 * gamma*lambda},  
			eligibility.get("action1")
		);
		assertArrayEquals(
			new double[] {0.2 * gamma*lambda, 0},  
			eligibility.get("action2")
		);
		
		double tdError = 1 - 0.1; //reward - q(s0,a1)
		// tests q-values: q = q + alpha * tderror * e )
		assertEquals(0.1 + alpha * tdError * (0.03+1), learner.qValue(features[0], "action1"));
		assertEquals(2 + alpha * tdError * 0.1, learner.qValue(features[1], "action1"));
		
		assertEquals(4 + alpha* tdError * 0.2, learner.qValue(features[0], "action2"));
		assertEquals(-1, learner.qValue(features[1], "action2"));
	}

	@Test
	/**
	 * This test verifies whether the learner INSIDE the unrestricted policy selection agent
	 * is properly saving and loading weights when the agent is called to do so
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 * @throws IOException
	 */
	void testLoadAndSaveWeights() throws NoSuchFieldException, IllegalAccessException, IOException {
		Map<String, double[]> testWeights = new HashMap<>(); 
		testWeights.put("action1", new double[] {0.1, 2});
		testWeights.put("action2", new double[] {4, -1});
		setLearnerWeights(testWeights);
		
		policySelector.saveWeights("testweights.bin");
		
		// changes some of the weights and verifies the change
		testWeights.get("action1")[0] = -1000;
		assertEquals(-1000, learner.qValue(new double[] {1, 0} , "action1"));
		
		// loads the previously saved weights
		policySelector.loadWeights("testweights.bin");
		
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
		Field field = learner.getClass().getDeclaredField("weights");
		field.setAccessible(true);
		field.set(learner, weights);
		assertEquals(learner.getWeights(), weights);
	}

}
