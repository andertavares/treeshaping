package main;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ai.core.AI;
import config.ConfigManager;
import config.Parameters;
import features.FeatureExtractor;
import features.FeatureExtractorFactory;
import learner.UnrestrictedPolicySelectionLearner;
import portfolio.PortfolioManager;
import reward.RewardModel;
import reward.RewardModelFactory;
import rts.GameSettings;
import rts.units.UnitTypeTable;
import utils.AILoader;

/**
 * FIXME unable to test a previously trained agent
 * @author anderson
 *
 */
public class Test {
	public static void main(String[] args) throws Exception {
		Logger logger = LogManager.getRootLogger();
		
		Options options = Parameters.testCommandLineOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("utility-name", options);

            System.exit(1);
        }

        String configFile = cmd.getOptionValue("config_input");
        String workingDir = cmd.getOptionValue("working_dir");
        
        Properties config = ConfigManager.loadConfig(configFile);
        
        // overrides config with command line parameters
        Parameters.mergeCommandLineIntoProperties(cmd, config);
        
        // ensures non-specified parameters get default values
        Parameters.ensureDefaults(config);
		
		// retrieves initial and final reps		
		int initialRep = Integer.parseInt(config.getProperty("initial_rep", "0"));
		int finalRep = Integer.parseInt(config.getProperty("final_rep", "0"));
				
		String testPartnerName = config.getProperty("test_opponent");
						
		boolean writeReplay = cmd.hasOption("save_replay");
		logger.info("Will {}save replays (.trace files).", writeReplay ? "" : "NOT ");
				
		for (int rep = initialRep; rep <= finalRep; rep++) {
			// determines the output dir according to the current rep
			String currentDir = workingDir + "/rep" + rep;
			
			// checks if that repetition has finished (otherwise it is not a good idea to test
			File repFinished = new File(currentDir + "/finished");
			if(! repFinished.exists()) {
				logger.warn("Repetition {} has not finished! Skipping...", rep);
				continue;
			}
			
			// finally runs one repetition
			// player 0's random seed increases whereas player 1's decreases with the repetitions  
			runTestMatches(config, testPartnerName, currentDir, rep, finalRep - rep + 1, writeReplay);
			
		}
	}
		
	/**
	 * This is basically a copy-and-paste from {@link Train.run} adapted to test matches
	 * TODO make the code more modular
	 * 
	 * @param configPath
	 * @param testPartnerName
	 * @param workingDir
	 * @param randomSeedP0
	 * @param randomSeedP1
	 * @param writeReplay write the replay (traces) for each match?
	 * @throws Exception
	 */
	public static void runTestMatches(Properties config, String testPartnerName, String workingDir, int randomSeedP0, int randomSeedP1, boolean writeReplay) throws Exception {
		Logger logger = LogManager.getRootLogger();
		
		int testMatches = Integer.parseInt(config.getProperty("test_matches"));
		
		int maxCycles = Integer.parseInt(config.getProperty("max_cycles"));
		int timeBudget = Integer.parseInt(config.getProperty("search.timebudget"));
		
        double epsilon = 0;
        double alpha = 0; //Double.parseDouble(config.getProperty("td.alpha.initial"));
        
        double gamma = Double.parseDouble(config.getProperty("td.gamma"));
        double lambda = Double.parseDouble(config.getProperty("td.lambda"));

        String portfolioNames = config.getProperty("portfolio");
        
        // loads microRTS game settings
     	GameSettings settings = GameSettings.loadFromConfig(config);
     		
        // creates a UnitTypeTable that should be overwritten by the one in config
        UnitTypeTable types = new UnitTypeTable(settings.getUTTVersion(), settings.getConflictPolicy());
        
        // loads the reward model (default=winlossdraw)
        RewardModel rewards = RewardModelFactory.getRewardModel(
 		   config.getProperty("rewards", "winlossdraw"), maxCycles
        );
        logger.debug("Reward model: {}", rewards.getClass().getSimpleName());
        
        FeatureExtractor featureExtractor = FeatureExtractorFactory.getFeatureExtractor(
 		   config.getProperty("features", "materialdistancehp"), types, maxCycles
        );
        logger.debug("Feature extractor: {}", featureExtractor.getClass().getSimpleName());
        
        List<String> selectionStrategies = Arrays.asList(
 		   config.getProperty("strategies", "CE,FE,HP-,HP+,AV+").split(",")
        );
        
        // creates the player instance and loads weights according to its position
        int testPosition = Integer.parseInt(config.getProperty("test_position", "0"));
        UnrestrictedPolicySelectionLearner player = new UnrestrictedPolicySelectionLearner(
			types, 
			PortfolioManager.getPortfolio(types, Arrays.asList(portfolioNames.split(","))),
			rewards,
			featureExtractor,
			selectionStrategies,
			maxCycles,
			timeBudget, alpha, epsilon, gamma, lambda,
			Integer.parseInt(config.getProperty("decision_interval")),
			randomSeedP0
		);
        String weightsFile = String.format("%s/weights_%d.bin", workingDir, testPosition);
        logger.info("Loading weights from {}", weightsFile);
		player.loadWeights(weightsFile);
		
		// updates the config with the overwritten parameters
		config.setProperty("random.seed.p0", Integer.toString(randomSeedP0));
		config.setProperty("random.seed.p1", Integer.toString(randomSeedP1));
		config.setProperty("portfolio", portfolioNames); //TODO isn't this handled in Parameters class?
		
		logger.info("This experiment's config: ");
		logger.info(config.toString());
		
		//config.store(new FileOutputStream(workingDir + "/settings.properties"), null);
		
		// test matches
		logger.info("Starting test...");
		boolean visualizeTest = Boolean.parseBoolean(config.getProperty("visualize_test", "false"));
		AI testOpponent = AILoader.loadAI(testPartnerName, types);
		
		logger.info("{} write replay.", writeReplay ? "Will" : "Will not");
		
		// if write replay (trace) is activated, sets the prefix to write files
		String tracePrefix = writeReplay ? workingDir + "/test-trace-vs-" + testOpponent.getClass().getSimpleName() : null;
		
		AI p0 = player, p1 = testOpponent;
		if(testPosition == 1) { //swaps the player and opponent if testPosition is activated
			p0 = testOpponent;
			p1 = player;
		}
		
		logger.info("Player0={}, Player1={}", p0.getClass().getSimpleName(), p1.getClass().getSimpleName());
		
		Runner.repeatedMatches(
			types, testMatches, 
			String.format("%s/test-vs-%s_p%d.csv", workingDir, testOpponent.getClass().getSimpleName(), testPosition), 
			p0, p1, visualizeTest, settings, tracePrefix
		);
		logger.info("Test finished.");
	}
}