package main;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom.JDOMException;

import ai.core.AI;
import players.A3N;
import rts.GameSettings;
import rts.GameSettings.LaunchMode;
import rts.units.UnitTypeTable;
import utils.CyclesCalculator;
import utils.StrategyNames;

/**
 * Runs A3N vs A1N in a given map
 * @author anderson
 *
 */
public class A3NvsA1N {

	public static void main(String[] args) throws JDOMException, IOException, Exception {
		Logger logger = LogManager.getRootLogger();
		
		Options options = new Options();

        options.addOption(new Option("m", "map_location", true, "Map to test"));
        options.addOption(new Option("d", "working_dir", true, "Directory to store results"));
        options.addOption(new Option("n", "num_matches", true, "Number of matches (half starting at each position)"));
        options.addOption(new Option("s", "strategy", true, "Unrestricted unit selection strategy (one of CC,CE,FC,FE,AV-,AV+,HP-,HP+,R,M)"));
        options.addOption(new Option("u", "unrestricted", true, "Number of unrestricted units"));
        
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("utility-name", options);
            System.exit(1);
        }
        
        String experimentDir = String.format("%s/%s/s%s-u%s", //all strings because they're retrieved from Property object 
    		cmd.getOptionValue("working_dir"), 
    		new File(cmd.getOptionValue("map_location")).getName().replaceFirst("[.][^.]+$", ""),  //map name without extension
    		cmd.getOptionValue("strategy"),
    		cmd.getOptionValue("unrestricted") 
		);
        
        GameSettings settings = new GameSettings(
    		LaunchMode.STANDALONE, "localhost", 0, 1, 
    		cmd.getOptionValue("map_location"), 
    		CyclesCalculator.calculate(cmd.getOptionValue("map_location")), 
    		false, //fully observable
    		UnitTypeTable.VERSION_ORIGINAL_FINETUNED, 
    		UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH, 
    		null, null
		);
        
        // number of matches can be specified via command line, but defaults to 40
        int numMatches = Integer.parseInt(cmd.getOptionValue("num_matches", "40")); 
        UnitTypeTable types = new UnitTypeTable(
    		settings.getUTTVersion(), settings.getConflictPolicy()
    	);
        
        String strategyName = StrategyNames.acronymToName(cmd.getOptionValue("strategy"));
        
        logger.info(
        	"Testing A3N with strategy {} ({}) and {} unrestricted units for {} matches.", 
        	cmd.getOptionValue("strategy"), strategyName, 
        	cmd.getOptionValue("unrestricted"),
        	numMatches
        );
        
        AI a3n = new A3N(
    		types, 
    		strategyName, 
    		Integer.parseInt(cmd.getOptionValue("unrestricted"))
		);
        AI a1n = new A3N( // uses A3N with 0 unrestricted units as a proxy for A1N
    		types, 
    		"ManagerFather", // should make no difference 
    		0				 // must be zero, otherwise will behave as A3N
		);//new A1N(types); 
        
        // creates a dummy config object with suitable parameters for the runner
        Properties dummyConfig = new Properties();
        dummyConfig.setProperty("working_dir", cmd.getOptionValue("working_dir"));
        dummyConfig.setProperty("checkpoint", "0");
        
        // run half the matches with p0 = a3n
        Runner.repeatedMatches(
			types, 
			experimentDir,
			numMatches / 2,
			experimentDir + "/A3N-vs-A1N.csv", 
			null, //won't record choices  
			a3n, a1n, 
			false, // won't visualize
			settings, 
			experimentDir + "/A3N-vs-A1N",	// will record traces
			0, //no checkpoints
			0 // assumes no previously played matches
		);
		logger.info("Finished running A3N as player 0 at {}", settings.getMapLocation());
		
		
		// run the other half with p1 = a3n
        Runner.repeatedMatches(
			types, 
			experimentDir,
			numMatches / 2,
			experimentDir + "/A1N-vs-A3N.csv", 
			null, //won't record choices  
			a1n, a3n, 
			false, // won't visualize
			settings, 
			experimentDir + "/A1N-vs-A3N",	// will record traces
			0, // no checkpoints
			0 // assumes no previously played matches
		);
        logger.info("Finished running A3N as player 1 at {}", settings.getMapLocation());
        
        logger.info("Done. Results are in {}", experimentDir);
	}

}
