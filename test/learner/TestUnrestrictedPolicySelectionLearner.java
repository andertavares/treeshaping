package learner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
	
	@BeforeEach
	void setUp() throws Exception {
		types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
		testFeatureExtractor = new MockupFeatureExtractor(new double[] {1.0, 0.5});
		testRewardModel = new MockupRewardModel(0.1, 0);
		
		learner = new UnrestrictedPolicySelectionLearner(
			types, 
			PortfolioManager.basicPortfolio(types), 
			testRewardModel, 
			testFeatureExtractor, 
			Arrays.asList(new String[] {"HP-","CE", "FC", "R"}), 
			3000, 
			100, 
			0.01, 0.1, 0.9, 0.1, 10, 0
		);
	}
	
	@Test
	void testQValue() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		// creates the test weights
		@SuppressWarnings("serial")
		Map<String, double[]> testWeights = new HashMap<>() {{
			put("action1", new double[] {0.3, 0.1});
		}};
		
		// opens the visibility of weights, sets them and tests the Q-value
		Field field = UnrestrictedPolicySelectionLearner.class.getDeclaredField("weights");
		field.setAccessible(true);
		field.set(learner, testWeights);
		
		//feature vector is [1.0, 0.5], weights for 'action1' are: [0.3, 0.1], expected Q-value is: 0.3 + 0.05 = 0.35
		assertEquals(0.35, learner.qValue(new double[] {1.0, 0.5}, "action1"));
		
	}
	
	@Test
	void testTDTarget() throws Exception {
		// creates the weights to test for qValue to return inside tdTarget
		@SuppressWarnings("serial")
		Map<String, double[]> testWeights = new HashMap<>() {{
			put("action1", new double[] {0.3, 0.1});
		}};
		
		// opens the visibility of weights, sets them and tests the Q-value
		Field field = UnrestrictedPolicySelectionLearner.class.getDeclaredField("weights");
		field.setAccessible(true);
		field.set(learner, testWeights);
		
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
	void testTDLambdaUpdateRule(){
		
	}
	
	@Test 
	void testSarsaUpdate() {
		
	}
	
	
	
	@Test
	void testGameOver() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	void testFromConfig() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	void testGetActionIntGameState() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	void testSaveWeights() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	void testLoadWeights() {
		fail("Not yet implemented"); // TODO
	}

}
