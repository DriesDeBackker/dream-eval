package evalapp.graphgenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import evalapp.master.Config;

public class GraphGenerator {
	private DependencyGraph graph;
	private Random r;

	public GraphGenerator(long seed) {
		this.r = new Random(seed);
	}

	public void generateGraph() {
		this.graph = new DependencyGraph();
		generateClients();
		generateVars();
		generateSignals();
	}

	public void setSeed(long seed) {
		this.r.setSeed(seed);
	}

	public DependencyGraph getGraph() {
		return this.graph;
	}

	private void generateClients() {
		for (int i = 1; i <= Config.NUMBER_OF_CLIENTS; i++) {
			String clientName = "host" + i;
			graph.addClient(clientName);
		}
		assert graph.getClients().size() == Config.NUMBER_OF_CLIENTS;
	}

	private void generateVars() {
		// Generate the specified number of vars.
		for (int i = 1; i <= Config.number_of_vars; i++) {
			String varName = "var" + i;
			String client = graph.getClients().get(r.nextInt(graph.getClients().size()));
			graph.addVar(varName, client);
		}
	}

	@SuppressWarnings("unused")
	private void generateSignals() {
		// Generate the specified number of graph levels.
		for (int l = 2; l <= Config.graph_depth; l++) {
			List<String> prevNodes = graph.getNodesUpTo(l - 1);
			// Generate a set of signals, respecting the average
			int signalsThisLevel = r.nextInt(2 * Config.signals_per_level_avg - 1) + 1;
			for (int s = 1; s <= signalsThisLevel; s++) {
				String signalName = "sig" + Integer.toString(l) + Integer.toString(s);
				List<String> deps = new ArrayList<String>();
				// Choose a set of dependencies, respecting the average
				int nbOfDeps;
				assert (Config.deps_per_signal_avg <= 3);
				if (Config.deps_per_signal_avg == 1) {
					nbOfDeps = 1;
				} else {
					nbOfDeps = r.nextInt(2) + Config.deps_per_signal_avg - 1;
				}
				// Choose one dependency from the previous level.
				deps.add(graph.getNodesAtLevel(l - 1).get(r.nextInt(graph.getNodesAtLevel(l - 1).size())));
				// Choose nodes from any previous level for other dependencies.
				for (int d = 1; d < nbOfDeps; d++) {
					boolean found = false;
					while (!found) {
						String chosenNode = prevNodes.get(r.nextInt(prevNodes.size()));
						if (!deps.contains(chosenNode)) {
							found = true;
							deps.add(chosenNode);
						}
					}
				}
				// Choose a host client respecting the average locality.
				String hostClient;
				boolean local = r.nextDouble() < Config.nodes_locality;
				Set<String> localClientsSet = new HashSet<String>();
				deps.forEach(d -> localClientsSet.add(graph.getHost(d)));
				List<String> localClientsList = new ArrayList<String>(localClientsSet);
				if (local) {
					hostClient = localClientsList.get(r.nextInt(localClientsList.size()));
				} else {
					List<String> remoteClientsList = new ArrayList<String>(graph.getClients());
					remoteClientsList.removeAll(localClientsSet);
					assert !remoteClientsList.isEmpty();
					hostClient = remoteClientsList.get(r.nextInt(remoteClientsList.size()));
				}
				graph.addSignal(signalName, hostClient, deps);
			}
		}
	}

}
