package learner;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jdom.JDOMException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import features.FeatureExtractor;
import portfolio.PortfolioManager;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

class TestUnrestrictedPolicySelectionLearner {

	UnrestrictedPolicySelectionLearner learner;
	FeatureExtractor testFeatureExtractor;
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
		lambda = 0.1;
		
		learner = new UnrestrictedPolicySelectionLearner(
			types, 
			PortfolioManager.basicPortfolio(types), 
			testRewardModel, 
			testFeatureExtractor, 
			Arrays.asList(new String[] {"HP-","CE", "FC", "R"}), 
			3000, 
			100, 
			alpha, 0.1, gamma, lambda, 10, 0
		);
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
	void testTDTarget() throws Exception {
		@SuppressWarnings("serial")
		Map<String, double[]> testWeights = new HashMap<>() {{
			put("action1", new double[] {0.3, 0.1});
		}};
		setLearnerWeights(testWeights);
		
		testRewardModel.setValues(0.1, 1.0);
		
		GameState state = new GameState(
			PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", types), 
			types
		); 
		
		// td target is r + gamma * q(s', a') -- q(s',a') is 0.35 (as per the previous test)
		assertEquals(0.1 + 0.9*0.35, learner.tdTarget(state, 0, "action1"));
		
		// now let's suppose the reached state is a gameover state
		state = new GameState(new PhysicalGameState(8, 8), types); //empty 8x8 physical game state is at gameover
		assertEquals(0.1, learner.tdTarget(state, 0, "action1"));
	}

	@Test 
	void testTDLambdaUpdateRule() throws JDOMException, IOException, Exception{
		// puts a custom set of weights into the learner
		@SuppressWarnings("serial")
		Map<String, double[]> testWeights = new HashMap<>() {{
			put("action1", new double[] {0.3, 0.1});
			put("action2", new double[] {0.7, 0.2});
		}};
		setLearnerWeights(testWeights);
		
		// creates fake" eligibility traces (all zeros) 
		@SuppressWarnings("serial")
		Map<String, double[]> eligibility = new HashMap<>() {{
			put("action1", new double[] {0, 0});
			put("action2", new double[] {0, 0});
		}};
		
		GameState previousState = new GameState(
			PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", types), 
			types
		); 
		
		GameState nextState = previousState.clone();
		
		// previousQ should be  1.0*0.3 + 0.5*0.1 = 0.35
		double previousQ = learner.qValue(new double[] {1.0, 0.5}, "action1");
		assertEquals(0.35, previousQ);
		
		// nextQ should be  1.0*0.4 + 0.5*0.2 = 0.8
		double nextQ = learner.qValue(new double[] {1.0, 0.5}, "action2");
		assertEquals(0.8, nextQ, 1E-5);
		
		// tdTarget should be r+gamma * nextQ = 0.1 + 0.9*0.8 = 0.82
		double tdTarget = learner.tdTarget(nextState, 0, "action2");
		assertEquals(0.82, tdTarget);
		
		double tdError =  tdTarget - previousQ; // (0.82-0.35) = 0.47
		
		// tests the update without eligibility
		learner.tdLambdaUpdateRule(previousState, 0, "action1", tdError, testWeights, eligibility);
		
		// tests if eligibility has changed (increased by the feature vector & decayed by gamma*lambda)
		assertArrayEquals(
			new double[] {1*gamma*lambda, 0.5*gamma*lambda}, 
			eligibility.get("action1")
		);
		assertArrayEquals(
			new double[] {0, 0}, 
			eligibility.get("action2")
		);
		
		
		// checks the weight vector for action1 
		double[] newWeightA1 = new double[] {0.3 + alpha*tdError, 0.1 + alpha*tdError*0.5};
		assertArrayEquals(
			newWeightA1, // update rule: w_i = w_i + alpha*error*e_i
			testWeights.get("action1")
		);
		
		// checks the weight vector for action2 (expected to be unchanged)
		assertArrayEquals(
			new double[] {0.7, 0.2}, 
			testWeights.get("action2")
		);
		
		// checks the q value
		assertEquals(
			newWeightA1[0] * 1.0 + newWeightA1[1]*0.5,
			learner.qValue(new double[] {1.0, 0.5}, "action1")
		);
		
	}
	
	@Test 
	void testSarsaUpdate() {
		
	}
	
	
	
	@Test
	void testGameOver() {
	}

	@Test
	void testFromConfig() {
	}

	@Test
	void testGetActionIntGameState() {
	}

	@Test
	void testSaveWeights() {
	}

	@Test
	void testLoadWeights() {
	}
	
	/**
	 * Sets the weights of our learner object
	 * @param weights
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 */
	private void setLearnerWeights(Map<String, double[]> weights) throws NoSuchFieldException, IllegalAccessException {
		
		// opens the visibility of weights, sets them and tests the Q-value
		Field field = UnrestrictedPolicySelectionLearner.class.getDeclaredField("weights");
		field.setAccessible(true);
		field.set(learner, weights);
		assertEquals(learner.getWeights(), weights);
	}

}
