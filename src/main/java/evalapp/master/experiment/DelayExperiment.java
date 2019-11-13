package evalapp.master.experiment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import dream.client.DreamClient;
import dream.client.RemoteVar;
import evalapp.commands.Experiment;
import evalapp.master.Update;

public class DelayExperiment extends ProgramDeployer {
	private static final String HOSTNAME = "master";
	private static final String[] clients = { "host1", "host2", "host3", "host4", "host5" };
	private static Experiment EXPERIMENT = Experiment.DELAY;

	protected List<RemoteVar<HashMap<String, List<Long>>>> varUpdateRemVars;
	protected List<RemoteVar<HashMap<String, List<Update>>>> finalNodeProxyUpdateRemVars;
	protected Map<String, List<Long>> varUpdates;
	protected Map<String, List<Update>> finalNodeProxyUpdates;
	protected double meanPropDelay;
	protected boolean resultsIn = false;

	public DelayExperiment() {
		super(HOSTNAME, EXPERIMENT);
		varUpdateRemVars = new ArrayList<>();
		finalNodeProxyUpdateRemVars = new ArrayList<>();
		varUpdates = new HashMap<>();
		finalNodeProxyUpdates = new HashMap<>();
	}

	protected List<String> waitForVars() {
		List<String> varsToWaitFor = new ArrayList<String>();
		for (int i = 0; i < clients.length; i++) {
			varsToWaitFor.add("ready@" + clients[i]);
			varsToWaitFor.add("varUpdates@" + clients[i]);
			varsToWaitFor.add("finalNodeUpdates@" + clients[i]);
		}
		return varsToWaitFor;
	}

	protected void prepareExperiment() {
		for (int i = 0; i < clients.length; i++) {
			RemoteVar<HashMap<String, List<Long>>> nrm = new RemoteVar<>(clients[i], "varUpdates");
			this.varUpdateRemVars.add(nrm);
			this.finalNodeProxyUpdateRemVars
					.add(new RemoteVar<HashMap<String, List<Update>>>(clients[i], "finalNodeUpdates"));
		}
	}

	@Override
	protected void endExperiment() {
		this.runVar.set(Boolean.FALSE);
		awaitResults();
		gatherResults();
		processResults();
	}

	private void awaitResults() {
		List<String> resultVars = new ArrayList<>();
		for (int i = 0; i < clients.length; i++) {
			resultVars.add("resultsSent@" + clients[i]);
		}
		while (!resultsIn(resultVars)) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private boolean resultsIn(List<String> resultVars) {
		for (String var : resultVars) {
			if (!DreamClient.instance.listVariables().contains(var)) {
				return false;
			} else {
				System.out.println(
						"Results of client" + Integer.toString(resultVars.indexOf(var) + 1) + " are available");
			}
		}
		return true;
	}

	private void gatherResults() {
		for (RemoteVar<HashMap<String, List<Long>>> rv : this.varUpdateRemVars) {
			Map<String, List<Long>> varLogs = rv.get();
			System.out.println("varLogs: " + varLogs.toString());
			for (String var : varLogs.keySet()) {
				this.varUpdates.put(var, varLogs.get(var));
			}
		}
		System.out.println("varUpdates: " + this.varUpdates.toString());
		for (RemoteVar<HashMap<String, List<Update>>> rv : this.finalNodeProxyUpdateRemVars) {
			Map<String, List<Update>> finalNodeLogs = rv.get();
			System.out.println("finalNodeUpdateLogs: " + finalNodeLogs.toString());
			for (String var : finalNodeLogs.keySet()) {
				this.finalNodeProxyUpdates.put(var, finalNodeLogs.get(var));
			}
		}
		System.out.println("finalNodeUpdates: " + this.finalNodeProxyUpdates.toString());
	}

	private void processResults() {
		// Get the final nodes for each var. Dispense with unused vars.
		System.out.println("vars: " + this.varUpdates.keySet().toString());
		Map<String, Set<String>> finalNodesOfVars = new HashMap<>();
		for (String var : new ArrayList<String>(this.varUpdates.keySet())) {
			Set<String> finalNodesOfVar = this.graph.getFinalNodesOf(var);
			if (finalNodesOfVar.isEmpty()) {
				this.varUpdates.remove(var);
			} else {
				finalNodesOfVars.put(var, finalNodesOfVar);
			}
		}

		System.out.println("finalNodesOfVars" + finalNodesOfVars.toString());

		List<Long> totalPropDelays = new ArrayList<>();
		for (String var : this.varUpdates.keySet()) {
			System.out.println("* Considering var: " + var);
			System.out.println("  update time list: " + this.varUpdates.get(var).toString());
			// Associate with each var the corresponding update streams of its
			// final nodes.
			Set<List<Long>> assocFinalNodeUpdateTimesForVar = new HashSet<>();
			for (String n : finalNodesOfVars.get(var)) {
				String proxyName = var + n;
				System.out.println("  -> Considering final node proxy: " + proxyName);
				List<Update> us = this.finalNodeProxyUpdates.get(proxyName).stream()
						.filter(u -> u.getSource().equals(var + "@" + this.graph.getHost(var)))
						.collect(Collectors.toList());
				System.out.println("     update list: " + us.toString());
				List<Long> ts = us.stream().map(u -> u.getTime()).collect(Collectors.toList());
				System.out.println("     update time list: " + ts.toString());
				assocFinalNodeUpdateTimesForVar.add(ts);
			}
			System.out.println(assocFinalNodeUpdateTimesForVar.toString());

			// Calculate the total propagation delay for each var update for
			// each var.
			List<Long> varUpdatesForVar = this.varUpdates.get(var);
			int length = varUpdatesForVar.size();
			for (List<Long> ts : assocFinalNodeUpdateTimesForVar) {
				if (ts.size() < length) {
					length = ts.size();
				}
			}
			for (int i = 0; i < length; i++) {
				Long longest = Long.valueOf(0);
				for (List<Long> ts : assocFinalNodeUpdateTimesForVar) {
					if (ts.get(i) > longest) {
						longest = ts.get(i);
					}
				}
				Long totalPropDelay = longest - varUpdatesForVar.get(i);
				totalPropDelays.add(totalPropDelay);
			}
		}
		System.out.println(totalPropDelays.toString());
		// Calculate the mean total propagation delay.
		Long sum = Long.valueOf(0);
		for (Long delay : totalPropDelays) {
			sum += delay;
		}
		double mean = sum / totalPropDelays.size();
		System.out.println("mean propagation delay: " + mean + "ms");
		this.meanPropDelay = mean;
	}
}
