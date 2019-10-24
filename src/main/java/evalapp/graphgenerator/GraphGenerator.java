package evalapp.graphgenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GraphGenerator {
	// Parameters with their (experiment) range (do not exceed).
	private final static int NUMBER_OF_CLIENTS = 5; // fixed
	private final static int NUMBER_OF_VARS = 5; // {1, 2, 5, 10}
	private final static int GRAPH_DEPTH = 4; // {2, 3, 4, 5, 10}}
	private final static int SIGNALS_PER_LEVEL_AVG = 2; // {1, 2, 5, 10}
	private final static int DEPS_PER_SIGNAL_AVG = 2; // {1, 2, 3}
	private final static double NODES_LOCALITY = 0.5; // {0,0.25,0.5,0.75,1.0}

	private Graph graph;
	private Random r;

	public GraphGenerator(long seed) {
		this.r = new Random(seed);
	}

	public void generateGraph() {
		setGraph(new Graph());
		generateClients();
		generateVars();
		generateSignals();
	}

	private void setGraph(Graph graph) {
		this.graph = graph;
	}

	public Graph getGraph() {
		return this.graph;
	}

	private void generateClients() {
		for (int i = 1; i <= NUMBER_OF_CLIENTS; i++) {
			String clientName = "client" + i;
			graph.addClient(clientName);
		}
		assert graph.getClients().size() == NUMBER_OF_CLIENTS;
	}

	private void generateVars() {
		// Generate the specified number of vars.
		for (int i = 1; i <= NUMBER_OF_VARS; i++) {
			String varName = "var" + i;
			String client = graph.getClients().get(r.nextInt(graph.getClients().size() - 1));
			graph.addVar(varName, client);
		}
	}

	private void generateSignals() {
		// Generate the specified number of graph levels.
		for (int l = 2; l <= GRAPH_DEPTH; l++) {
			List<String> prevNodes = graph.getNodesUpTo(l - 1);
			// Generate a set of signals, respecting the average
			int signalsThisLevel = r.nextInt(2 * SIGNALS_PER_LEVEL_AVG - 2) + 1;
			for (int s = 1; s <= signalsThisLevel; s++) {
				String signalName = "sig" + Integer.toString(l) + Integer.toString(s);
				List<String> deps = new ArrayList<String>();
				// Choose a set of dependencies, respecting the average
				int nbOfDeps;
				assert (DEPS_PER_SIGNAL_AVG <= 3);
				if (DEPS_PER_SIGNAL_AVG == 1) {
					nbOfDeps = 1;
				} else {
					nbOfDeps = r.nextInt(2) + DEPS_PER_SIGNAL_AVG - 1;
				}
				// Choose one dependency from the previous level.
				deps.add(graph.getNodesAtLevel(l - 1).get(r.nextInt(graph.getNodesAtLevel(l - 1).size() - 1)));
				// Choose nodes from any previous level for other dependencies.
				for (int d = 1; d < nbOfDeps; d++) {
					boolean found = false;
					while (!found) {
						String chosenNode = prevNodes.get(r.nextInt(prevNodes.size()) - 1);
						if (!deps.contains(chosenNode)) {
							found = true;
							deps.add(chosenNode);
						}
					}
				}
				// Choose a host client respecting the average locality.
				String hostClient;
				boolean local = r.nextDouble() > NODES_LOCALITY;
				Set<String> localClientsSet = new HashSet<String>();
				deps.forEach(d -> localClientsSet.add(graph.getHost(d)));
				List<String> localClientsList = new ArrayList<String>(localClientsSet);
				if (local) {
					hostClient = localClientsList.get(r.nextInt(localClientsList.size() - 1));
				} else {
					List<String> remoteClientsList = new ArrayList<String>(graph.getClients());
					remoteClientsList.removeAll(localClientsSet);
					assert !remoteClientsList.isEmpty();
					hostClient = remoteClientsList.get(r.nextInt(remoteClientsList.size() - 1));
				}
				graph.addSignal(signalName, hostClient, deps);
			}
		}
	}

}
