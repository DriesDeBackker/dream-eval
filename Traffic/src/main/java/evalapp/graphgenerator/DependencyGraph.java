package evalapp.graphgenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DependencyGraph {

	private final List<String> clients;
	private Map<String, String> varLocationsByName;
	private Map<String, String> signalLocationsByName;
	private Map<String, List<String>> signalDepsByName;
	private List<List<String>> nodesByLevel;

	public DependencyGraph() {
		clients = new ArrayList<String>();
		varLocationsByName = new HashMap<String, String>();
		signalLocationsByName = new HashMap<String, String>();
		signalDepsByName = new HashMap<String, List<String>>();
		nodesByLevel = new ArrayList<List<String>>();
	}

	public void addClient(String clientName) {
		assert (!clients.contains(clientName));
		clients.add(clientName);
	}

	public List<String> getClients() {
		return clients;
	}

	public void addVar(String varName, String varHost) {
		assert (!varLocationsByName.containsKey(varName));
		varLocationsByName.put(varName, varHost);
		if (nodesByLevel.isEmpty()) {
			List<String> firstLevel = new ArrayList<String>();
			nodesByLevel.add(firstLevel);
		}
		nodesByLevel.get(0).add(varName);
		assert this.getNodes().size() == varLocationsByName.size() + signalLocationsByName.size();
		assert nodesByLevel.get(0).size() == varLocationsByName.size();
	}

	public Map<String, String> getVars() {
		return varLocationsByName;
	}

	public void addSignal(String sigName, String sigHost, List<String> sigDeps) {
		int level = this.getMaxLevel(sigDeps) + 1;
		signalLocationsByName.put(sigName, sigHost);
		signalDepsByName.put(sigName, sigDeps);
		if (nodesByLevel.size() < level) {
			List<String> newLevel = new ArrayList<String>();
			newLevel.add(sigName);
			nodesByLevel.add(newLevel);
		} else {
			nodesByLevel.get(level - 1).add(sigName);
		}
		assert this.getNodes().size() == varLocationsByName.size() + signalLocationsByName.size();
		assert signalLocationsByName.size() == signalDepsByName.size();
	}

	public int getLevel(String nodeName) {
		assert this.getNodes().contains(nodeName);
		for (int l = 1; l <= nodesByLevel.size(); l++) {
			List<String> currentLevel = nodesByLevel.get(l - 1);
			if (currentLevel.contains(nodeName)) {
				return l;
			}
		}
		// Should never reach this point.
		return Integer.MIN_VALUE;
	}

	private int getMaxLevel(List<String> nodeNames) {
		int max = 0;
		for (String name : nodeNames) {
			int level = this.getLevel(name);
			if (level > max) {
				max = level;
			}
		}
		return max;
	}

	public Map<String, String> getSignals() {
		return signalLocationsByName;
	}

	public List<String> getSignalDeps(String sigName) {
		return signalDepsByName.get(sigName);
	}

	public List<String> getNodes() {
		List<String> nodes = new ArrayList<String>();
		nodesByLevel.forEach(l -> nodes.addAll(l));
		return nodes;
	}

	public List<String> getNodesUpTo(int level) {
		List<String> nodes = new ArrayList<String>();
		int maxLevel = Math.min(level, nodesByLevel.size());
		for (int l = 1; l <= maxLevel; l++) {
			nodes.addAll(nodesByLevel.get(l - 1));
		}
		return nodes;
	}

	public List<String> getNodesAtLevel(int level) {
		assert (0 <= level && level <= nodesByLevel.size());
		return nodesByLevel.get(level - 1);
	}

	public String getHost(String nodeName) {
		assert this.getNodes().contains(nodeName);
		if (varLocationsByName.containsKey(nodeName)) {
			return varLocationsByName.get(nodeName);
		} else {
			return signalLocationsByName.get(nodeName);
		}
	}

	public int getNumberOfLevels() {
		return this.nodesByLevel.size();
	}

	public boolean isFinal(String nodeName) {
		for (List<String> deps : this.signalDepsByName.values()) {
			for (String dep : deps) {
				if (nodeName.equals(dep)) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean isVar(String nodeName) {
		return this.getVars().containsKey(nodeName);
	}

	public Set<String> getFinalNodesOf(String var) {
		Set<String> closureNodes = new HashSet<>();
		closureNodes.add(var);
		for (int l = 2; l <= getNumberOfLevels(); l++) {
			Set<String> additions = new HashSet<>();
			for (String node : this.getNodesAtLevel(l)) {
				for (String dep : this.getSignalDeps(node)) {
					if (closureNodes.contains(dep)) {
						additions.add(node);
					}
				}
			}
			closureNodes.addAll(additions);
		}
		Set<String> finalNodes = closureNodes.parallelStream().filter(n -> this.isFinal(n) && !this.isVar(n))
				.collect(Collectors.toSet());
		return finalNodes;
	}
}
