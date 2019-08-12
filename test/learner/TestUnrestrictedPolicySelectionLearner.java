package learner;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import features.FeatureExtractor;
import portfolio.PortfolioManager;
import reward.RewardModelFactory;
import rts.units.UnitTypeTable;

class TestUnrestrictedPolicySelectionLearner {

	UnrestrictedPolicySelectionLearner learner;
	FeatureExtractor testFeatureExtractor = new MockupFeatureExtractor(new double[] {1.0, 0.5});
	
	@BeforeEach
	void setUp() throws Exception {
		UnitTypeTable types = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
		
		learner = new UnrestrictedPolicySelectionLearner(
			types, 
			PortfolioManager.basicPortfolio(types), 
			RewardModelFactory.getRewardModel("winlossdraw", 3000), 
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
	void testTDTarget() {
		
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
