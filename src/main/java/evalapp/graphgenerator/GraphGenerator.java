package evalapp.graphgenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class GraphGenerator {
	// Parameters
	private final static int NUMBER_OF_CLIENTS = 5;
	private final static int NUMBER_OF_VARS = 5;
	private final static int GRAPH_DEPTH = 4;
	private final static int AVERAGE_NUMBER_OF_SIGNALS_PER_LEVEL = 2;
	private final static int AVERAGE_NUMBER_OF_DEPENDENCIES_PER_SIGNAL = 2;
	private final static double DEGREE_OF_NODES_LOCALITY = 0.5;

	// Graph Representation
	private final List<String> clients = new ArrayList<String>();
	private Map<String, String> varLocationsByName = new HashMap<String, String>();
	private Map<String, String> signalLocationsByName = new HashMap<String, String>();
	private Map<String, List<String>> signalDepsByName = new HashMap<String, List<String>>();
	private List<List<String>> nodesByLevel = new ArrayList<List<String>>();
	private List<String> nodes = new ArrayList<String>();

	// Random generator
	private Random r;

	public GraphGenerator(long seed) {
		this.r = new Random(seed);
	}

	public void generateGraph() {
		generateClients();
		generateVars();
		generateSignals();
	}

	private void generateClients() {
		for (int i = 1; i <= NUMBER_OF_CLIENTS; i++) {
			String clientName = "client" + i;
			clients.add(clientName);
		}
		assert clients.size() == NUMBER_OF_CLIENTS;

	}

	private void generateVars() {
		// Generate the specified number of vars.
		List<String> firstLevel = new ArrayList<String>();
		for (int i = 1; i <= NUMBER_OF_VARS; i++) {
			String varName = "var" + i;
			firstLevel.add(varName);
			String client = clients.get(r.nextInt(4));
			varLocationsByName.put(varName, client);
		}
		nodesByLevel.add(firstLevel);
		for (String n : firstLevel) {
			nodes.add(n);
		}
	}

	private void generateSignals() {
		// Generate the specified number of graph levels.
		for (int l = 0; l < GRAPH_DEPTH; l++) {
			List<String> newLevelSignals = new ArrayList<String>();
			List<String> prevLevelNodes = nodesByLevel.get(l - 1);
			Map<String, List<String>> newLevelSignalDeps = new HashMap<String, List<String>>();
			Map<String, String> newLevelSignalHosts = new HashMap<String, String>();
			// Generate a set of signals, respecting the average
			// number
			int signalsThisLevel = r.nextInt(2) + 1;
			for (int s = 1; s <= signalsThisLevel; s++) {
				String signalName = "sig" + l + s;
				List<String> deps = new ArrayList<String>();
				// Choose a set of dependencies, respecting the
				// average number
				int nbOfDeps = r.nextInt(2) + 1;
				// Choose at least one dependency from the previous level.
				deps.add(prevLevelNodes.get(r.nextInt(prevLevelNodes.size() - 1)));
				// Choose random nodes for the other dependencies, if any.
				for (int d = 1; d < nbOfDeps; d++) {
					boolean found = false;
					while (!found) {
						String chosenNode = nodes.get(r.nextInt(nodes.size()) - 1);
						if (!deps.contains(chosenNode)) {
							found = true;
							deps.add(chosenNode);
						}
					}
				}
				// Choose a host client respecting the average locality.
				String hostClient;
				boolean local = r.nextDouble() > DEGREE_OF_NODES_LOCALITY;
				Set<String> localClientsSet = new HashSet<String>();
				deps.forEach(d -> localClientsSet.add(varLocationsByName.get(d)));
				deps.forEach(d -> localClientsSet.add(signalLocationsByName.get(d)));
				localClientsSet.remove(null);
				List<String> localClientsList = new ArrayList<String>(localClientsSet);
				if (local) {
					hostClient = localClientsList.get(r.nextInt(localClientsList.size() - 1));
				} else {
					List<String> remoteClientsList = new ArrayList<String>(clients);
					remoteClientsList.removeAll(localClientsSet);
					assert !remoteClientsList.isEmpty();
					hostClient = remoteClientsList.get(r.nextInt(remoteClientsList.size() - 1));
				}
				newLevelSignals.add(signalName);
				newLevelSignalDeps.put(signalName, deps);
				newLevelSignalHosts.put(signalName, hostClient);

			}

			for (String n : newLevelSignals) {
				List<String> deps = newLevelSignalDeps.get(n);
				String host = newLevelSignalHosts.get(n);
				nodes.add(n);
				signalLocationsByName.put(n, host);
				signalDepsByName.put(n, deps);
			}
			nodesByLevel.add(newLevelSignals);
		}
	}

}
