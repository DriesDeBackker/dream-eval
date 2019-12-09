package evalapp.master;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import dream.client.DreamClient;
import dream.client.RemoteVar;
import dream.client.UpdateProducer;
import dream.client.Var;
import evalapp.commands.Command;
import evalapp.commands.CommandsGenerator;
import evalapp.commands.Experiment;
import evalapp.commands.IterationSpecifics;
import evalapp.commands.VarCommand;
import evalapp.graphgenerator.DependencyGraph;
import evalapp.graphgenerator.GraphGenerator;
import evalapp.graphgenerator.GraphVisualizer;
import evalapp.valgenerator.RandomNormalInteger;
import evalapp.valgenerator.ValueGenerator;

public class DelayExperiment extends Client {
	protected static final String[] clients = { "host1", "host2", "host3", "host4", "host5" };

	protected Var<Command> cmdsVar;
	protected Var<Boolean> runningVar;
	protected Var<Boolean> emittingVar;
	protected Var<Experiment> experimentVar;
	protected GraphGenerator gg;
	protected CommandsGenerator<Integer> cg;
	protected IterationSpecifics is;
	protected ValueGenerator<Integer> vg;
	protected List<Command> cmds;
	protected Experiment exp = Experiment.DELAY;
	protected DependencyGraph graph;

	private static final String HOSTNAME = "master";

	protected List<RemoteVar<HashMap<String, List<Long>>>> varUpdateRemVars;
	protected List<RemoteVar<HashMap<String, List<Update>>>> finalNodeProxyUpdateRemVars;
	protected Map<String, List<Long>> varUpdates;
	protected Map<String, List<Update>> finalNodeProxyUpdates;
	protected double meanPropDelay;
	protected boolean resultsIn = false;

	public DelayExperiment() {
		super(HOSTNAME);
	}

	@Override
	protected void init() {
		this.cmdsVar = new Var<Command>("commands", null);
		this.runningVar = new Var<Boolean>("running", null);
		this.emittingVar = new Var<Boolean>("emitting", null);
		this.experimentVar = new Var<Experiment>("experiment", null);
		this.is = new IterationSpecifics(Config.update_interval_mean, Config.update_interval_sd);
		// this.is = new IterationSpecifics(1000, 0);
		this.gg = new GraphGenerator(Config.random_seed);
		this.vg = new RandomNormalInteger(Config.values_mean, Config.values_sd);

		varUpdateRemVars = new ArrayList<>();
		finalNodeProxyUpdateRemVars = new ArrayList<>();
		varUpdates = new HashMap<>();
		finalNodeProxyUpdates = new HashMap<>();
	}

	public void run() {
		createProgram();
		deployProgram(cmds);
		prepareExperiment();
		startExperiment();
		endExperiment();
	}

	@Override
	protected List<String> waitForVars() {
		List<String> varsToWaitFor = new ArrayList<String>();
		for (int i = 0; i < clients.length; i++) {
			varsToWaitFor.add("ready@" + clients[i]);
			varsToWaitFor.add("varUpdates@" + clients[i]);
			varsToWaitFor.add("finalNodeUpdates@" + clients[i]);
		}
		return varsToWaitFor;
	}

	@SuppressWarnings("unchecked")
	protected void createProgram() {
		gg.generateGraph();
		this.graph = gg.getGraph();
		GraphVisualizer gv = new GraphVisualizer(graph);
		gv.visualize();
		List<Function<UpdateProducer<Integer>, ?>> fns = new ArrayList<Function<UpdateProducer<Integer>, ?>>();
		Function<UpdateProducer<Integer>, Integer> unary = (Function<UpdateProducer<Integer>, Integer> & Serializable) a -> a
				.get();
		fns.add(unary);
		Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Integer>> binary = (Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Integer>> & Serializable) a -> b -> {
			List<Integer> vals = new ArrayList<>();
			vals.add(a.get());
			vals.add(b.get());
			vals.removeAll(Collections.singleton(null));
			Integer sum = 0;
			for (Integer v : vals) {
				sum += v;
			}
			return Math.round(sum / vals.size());
		};
		fns.add(binary);
		Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Integer>>> ternary = (Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Integer>>> & Serializable) a -> b -> c -> {
			List<Integer> vals = new ArrayList<>();
			vals.add(a.get());
			vals.add(b.get());
			vals.add(c.get());
			vals.removeAll(Collections.singleton(null));
			Integer sum = 0;
			for (Integer v : vals) {
				sum += v;
			}
			return Math.round(sum / vals.size());
		};
		fns.add(ternary);

		Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Integer>>>> quaternary = (Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Integer>>>> & Serializable) a -> b -> c -> d -> {
			List<Integer> vals = new ArrayList<>();
			vals.add(a.get());
			vals.add(b.get());
			vals.add(c.get());
			vals.add(d.get());
			vals.removeAll(Collections.singleton(null));
			Integer sum = 0;
			for (Integer v : vals) {
				sum += v;
			}
			return Math.round(sum / vals.size());
		};

		fns.add(quaternary);
		this.cg = new CommandsGenerator<Integer>(exp, graph, this.vg, this.is, fns);
		cg.generateCommands();
		this.cmds = cg.getCommands();
		System.out.println(this.cmds.toString());
	}

	@SuppressWarnings("unchecked")
	protected void deployProgram(List<Command> cmds) {
		System.out.println("Deploying distributed reactive program");

		this.experimentVar.set(this.exp);

		for (Command cmd : cmds) {
			if (cmd instanceof VarCommand) {
				VarCommand<Integer> vcmd = (VarCommand<Integer>) cmd;
				System.out.println("Now sending over a VarCommand for: " + vcmd.getName() + " with initial value: "
						+ vcmd.getInitialValue());
			}
			cmdsVar.set(cmd);
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	protected void prepareExperiment() {
		for (int i = 0; i < clients.length; i++) {
			RemoteVar<HashMap<String, List<Long>>> nrm = new RemoteVar<>(clients[i], "varUpdates");
			this.varUpdateRemVars.add(nrm);
			this.finalNodeProxyUpdateRemVars
					.add(new RemoteVar<HashMap<String, List<Update>>>(clients[i], "finalNodeUpdates"));
		}
	}

	private void startExperiment() {
		this.runningVar.set(Boolean.TRUE);
		this.emittingVar.set(Boolean.TRUE);

		System.out.println("EXPERIMENT STARTED");

		try {
			Thread.sleep(Config.experiment_length);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected void endExperiment() {
		this.runningVar.set(Boolean.FALSE);
		this.emittingVar.set(Boolean.FALSE);
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

		List<Double> totalPropDelays = new ArrayList<>();
		for (String var : this.varUpdates.keySet()) {
			List<Long> varPropDelays = new ArrayList<>();
			System.out.println("* Considering var: " + var);
			System.out.println("  update time list: " + this.varUpdates.get(var).toString());
			// Associate with each var the corresponding update streams of its
			// final nodes.
			Set<Map<Long, Long>> assocFinalNodeUpdateTimesForVar = new HashSet<>();
			for (String n : finalNodesOfVars.get(var)) {
				String proxyName = var + n;
				System.out.println("  -> Considering final node proxy: " + proxyName);
				List<Update> us = this.finalNodeProxyUpdates.get(proxyName).stream()
						.filter(u -> u.getSource().equals(var + "@" + this.graph.getHost(var)))
						.collect(Collectors.toList());
				System.out.println("     update list: " + us.toString());
				Map<Long, Long> lastEndTimePerSourceTime = new HashMap<>();
				for (Update u : us) {
					Long lastEndTime = lastEndTimePerSourceTime.get(u.getSourceTime());
					if (lastEndTime == null) {
						lastEndTime = u.getEndTime();
					} else {
						lastEndTime = Long.max(lastEndTime, u.getEndTime());
					}
					lastEndTimePerSourceTime.put(u.getSourceTime(), lastEndTime);
				}
				assocFinalNodeUpdateTimesForVar.add(lastEndTimePerSourceTime);
			}
			// Calculate the total propagation delay for each var update.
			List<Long> varUpdatesForVar = this.varUpdates.get(var);

			for (Long sourceTime : varUpdatesForVar) {
				Long lastEndTime = Long.valueOf(0);
				for (Map<Long, Long> lastEndTimePerSourceTime : assocFinalNodeUpdateTimesForVar) {
					if (lastEndTime != null) {
						Long endTime = lastEndTimePerSourceTime.get(sourceTime);
						if (endTime == null) {
							lastEndTime = null;
						} else {
							lastEndTime = Long.max(lastEndTime, endTime);
						}
					}
				}
				if (lastEndTime != null) {
					Long totalPropDelay = lastEndTime - sourceTime;
					varPropDelays.add(totalPropDelay);
				}
			}
			varPropDelays = varPropDelays.stream().filter(d -> d < 1500).collect(Collectors.toList());
			System.out.println(varPropDelays.toString());
			Long sum = Long.valueOf(0);
			for (Long delay : varPropDelays) {
				sum += delay;
			}
			double mean = sum / varPropDelays.size();
			totalPropDelays.add(mean);
		}
		System.out.println(totalPropDelays.toString());
		// Calculate the mean total propagation delay.
		double sum = Double.valueOf(0);
		for (Double delay : totalPropDelays) {
			sum += delay;
		}
		double mean = sum / totalPropDelays.size();
		System.out.println("mean propagation delay: " + mean + "ms");
		this.meanPropDelay = mean;
	}
}
