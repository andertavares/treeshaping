package learning;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import features.FeatureExtractor;
import rts.GameState;

public class MockupFeatureExtractor implements FeatureExtractor {

	Map<GameState, double[]> mapping;
	
	double[] featureVector;
	
	/**
	 * Initializes with the specified feature vector
	 * @param featureVector
	 * @param numFeatures
	 */
	public MockupFeatureExtractor(double[] featureVector) {
		mapping = new HashMap<>();
		
		setFeatureVector(featureVector);
	}
	
	public void setFeatureVector(double[] features) {
		featureVector = features;
	}
	
	/**
	 * Adds a pair (state, features). These features will be returned when
	 * {@link #extractFeatures(GameState, int)} is called with the 
	 * corresponding state.
	 * FIXME does not work if state is cloned
	 * @param state
	 * @param features
	 */
	public void putMapping(GameState state, double[] features) {
		mapping.put(state, features);
	}
	
	@Override
	/**
	 * Just returns the length of a previously set feature vector
	 */
	public int getNumFeatures() {
		return featureVector.length;
	}

	@Override
	/**
	 * A simple method that returns the previously set feature vector for the given state
	 * (or the default feature vector if no mapping was previously set)
	 */
	public double[] extractFeatures(GameState s, int player) {
		if (mapping.size() == 0) {
			return featureVector;
		}
		
		// attempts to prevent error when state stored in mapping is cloned
		for(Entry<GameState,double[]> e : mapping.entrySet() ) {
			if(e.getKey().equals(s)) {
				return e.getValue();
			}
		}
		
		return null; //should not get here state not found in mapping
		
	}

	@Override
	public List<String> featureNames() {
		// should not be called...
		return null;
	}

}
