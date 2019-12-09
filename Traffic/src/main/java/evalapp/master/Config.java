package evalapp.master;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import evalapp.commands.Experiment;

public class Config {

	private static final Properties properties = new Properties();

	public final static int NUMBER_OF_CLIENTS = 5; // fixed
	public final static int number_of_vars; // {1, 2, 5, 10}
	public final static int graph_depth; // {2, 3, 4, 5, 10}}
	public final static int signals_per_level_avg; // {1, 2, 5, 10}
	public final static int deps_per_signal_avg; // {1, 2, 3}
	public final static double nodes_locality; // {0,0.25,0.5,0.75,1.0}
	public final static int update_interval_mean;
	public final static int update_interval_sd;
	public final static int values_mean;
	public final static int values_sd;
	public final static int random_seed;
	public final static int experiment_length;
	public final static Experiment experiment = Experiment.TRAFFIC;

	static {

		// Load properties from config file.
		try {
			final FileInputStream input = new FileInputStream("evalconfig.properties");
			properties.load(input);
			input.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		// Parse the properties and import them as fields.
		final String numberOfVarsProperty = properties.getProperty("numberOfVars", "5");
		final String graphDepthProperty = properties.getProperty("graphDepth", "4");
		final String signalsPerLevelAvgProperty = properties.getProperty("signalsPerLevelAvg", "2");
		final String depsPerSignalAvgProperty = properties.getProperty("depsPerSignalAvg", "2");
		final String nodesLocalityProperty = properties.getProperty("nodesLocality", "0.5");
		final String updateIntervalMeanProperty = properties.getProperty("updateIntervalMean", "1000");
		final String updateIntervalSDProperty = properties.getProperty("updateIntervalSD", "100");
		final String valuesMeanProperty = properties.getProperty("valuesMean", "100");
		final String valuesSDProperty = properties.getProperty("valuesSD", "20");
		final String randomSeedProperty = properties.getProperty("randomSeed", "1815");
		final String experimentLengthProperty = properties.getProperty("experimentLength", "600000");

		number_of_vars = Integer.parseInt(numberOfVarsProperty);
		graph_depth = Integer.parseInt(graphDepthProperty);
		signals_per_level_avg = Integer.parseInt(signalsPerLevelAvgProperty);
		deps_per_signal_avg = Integer.parseInt(depsPerSignalAvgProperty);
		nodes_locality = Double.parseDouble(nodesLocalityProperty);
		update_interval_mean = Integer.parseInt(updateIntervalMeanProperty);
		update_interval_sd = Integer.parseInt(updateIntervalSDProperty);
		values_mean = Integer.parseInt(valuesMeanProperty);
		values_sd = Integer.parseInt(valuesSDProperty);
		random_seed = Integer.parseInt(randomSeedProperty);
		experiment_length = Integer.parseInt(experimentLengthProperty);

	}
}
