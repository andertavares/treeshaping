package learner;

import java.util.List;

import features.FeatureExtractor;
import rts.GameState;

public class MockupFeatureExtractor implements FeatureExtractor {

	double[] featureVector;
	
	/**
	 * Initializes with the specified feature vector
	 * @param featureVector
	 * @param numFeatures
	 */
	public MockupFeatureExtractor(double[] featureVector) {
		setFeatureVector(featureVector);
	}
	
	public void setFeatureVector(double[] features) {
		featureVector = features;
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
	 * A simple method that just returns the previously set feature vector
	 */
	public double[] extractFeatures(GameState s, int player) {
		return featureVector;
	}

	@Override
	public List<String> featureNames() {
		// should not be called...
		return null;
	}

}
