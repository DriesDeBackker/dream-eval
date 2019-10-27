package evalapp.master;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import dream.client.RemoteVar;
import dream.client.UpdateProducer;
import dream.client.Var;
import evalapp.commands.Command;
import evalapp.commands.CommandsGenerator;
import evalapp.commands.EndCommand;
import evalapp.commands.IterationSpecifics;
import evalapp.commands.StartCommand;
import evalapp.commands.VarCommand;
import evalapp.graphgenerator.DependencyGraph;
import evalapp.graphgenerator.GraphGenerator;
import evalapp.graphgenerator.GraphVisualizer;
import evalapp.valgenerator.RandomNormalInteger;
import evalapp.valgenerator.ValueGenerator;

public class ProgramDeployer extends Client {
	private static final String HOSTNAME = "master";
	private static final String[] clients = { "host1", "host2", "host3", "host4", "host5" };

	private Var<Command> cmdsVar;
	private GraphGenerator gg;
	private DependencyGraph graph;
	private CommandsGenerator<Integer> cg;
	private IterationSpecifics is;
	private ValueGenerator<Integer> vg;
	private List<Command> cmds;

	List<RemoteVar<HashMap<String, List<Long>>>> varUpdateRemVars = new ArrayList<>();
	List<RemoteVar<HashMap<String, List<Update>>>> finalNodeUpdateRemVars = new ArrayList<>();;
	Map<String, List<Long>> varUpdates = new HashMap<>();;
	Map<String, List<Update>> finalNodeUpdates = new HashMap<>();;
	private double meanPropDelay;

	public ProgramDeployer() {
		super(HOSTNAME);
		deployProgram(cmds);
		askPermissionToStart();
		runExperiment();
		gatherResults();
		processResults();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void init() {
		this.cmdsVar = new Var<Command>("commands", null);
		this.is = new IterationSpecifics(Config.update_interval_mean, Config.update_interval_sd);
		this.gg = new GraphGenerator(Config.random_seed);
		gg.generateGraph();
		this.graph = gg.getGraph();
		GraphVisualizer gv = new GraphVisualizer(graph);
		gv.visualize();
		this.vg = new RandomNormalInteger(Config.values_mean, Config.values_sd);
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
		this.cg = new CommandsGenerator<Integer>(graph, this.vg, this.is, fns);
		cg.generateCommands();
		this.cmds = cg.getCommands();
		System.out.println(this.cmds.toString());
	}

	@Override
	protected List<String> waitForVars() {
		List<String> varsToWaitFor = new ArrayList<String>();
		for (int i = 0; i < clients.length; i++) {
			varsToWaitFor.add("varUpdates@" + clients[i]);
			varsToWaitFor.add("finalNodeUpdates@" + clients[i]);
		}
		return varsToWaitFor;
	}

	private void deployProgram(List<Command> cmds) {
		System.out.println("Deploying distributed reactive program");
		for (Command cmd : cmds) {
			if (cmd instanceof VarCommand) {
				VarCommand<Integer> vcmd = (VarCommand<Integer>) cmd;
				System.out.println("Now sending over a VarCommand for: " + vcmd.getName() + " with initial value: "
						+ vcmd.getInitialValue());
			}
			cmdsVar.set(cmd);
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		for (int i = 1; i < clients.length; i++) {
			varUpdateRemVars.add(new RemoteVar<HashMap<String, List<Long>>>(clients[i], "varUpdates"));
			finalNodeUpdateRemVars.add(new RemoteVar<HashMap<String, List<Update>>>(clients[i], "finalNodeUpdates"));
		}
	}

	private void askPermissionToStart() {
		Scanner reader = new Scanner(System.in); // Reading from System.in
		System.out.println("Input any number to start the experiment: ");
		reader.nextInt();
		reader.close();
	}

	private void runExperiment() {
		try {
			for (int i = 1; i <= clients.length; i++) {
				cmdsVar.set(new StartCommand("host" + i));
				Thread.sleep(250);
			}
			Thread.sleep(Config.experiment_length);
			for (int i = 1; i <= clients.length; i++) {
				cmdsVar.set(new EndCommand("host" + i));
				Thread.sleep(250);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void gatherResults() {
		for (RemoteVar<HashMap<String, List<Long>>> rv : varUpdateRemVars) {
			Map<String, List<Long>> varLogs = rv.get();
			for (String var : varLogs.keySet()) {
				varUpdates.put(var, varLogs.get(var));
			}
		}
		System.out.println("varUpdates: " + varUpdates.toString());
		for (RemoteVar<HashMap<String, List<Update>>> rv : finalNodeUpdateRemVars) {
			Map<String, List<Update>> finalNodeLogs = rv.get();
			for (String var : finalNodeLogs.keySet()) {
				finalNodeUpdates.put(var, finalNodeLogs.get(var));
			}
		}
		System.out.println("finalNodeUpdates: " + finalNodeUpdates.toString());
	}

	private void processResults() {
		// Get the final nodes for each var. Dispense with unused vars.
		System.out.println("vars: " + varUpdates.keySet().toString());
		Map<String, Set<String>> finalNodesOfVars = new HashMap<>();
		for (String var : new ArrayList<String>(varUpdates.keySet())) {
			Set<String> finalNodesOfVar = graph.getFinalNodesOf(var);
			if (finalNodesOfVar.isEmpty()) {
				varUpdates.remove(var);
			} else {
				finalNodesOfVars.put(var, graph.getFinalNodesOf(var));
			}
		}

		System.out.println("finalNodesOfVars" + finalNodesOfVars.toString());

		List<Long> totalPropDelays = new ArrayList<>();
		for (String var : varUpdates.keySet()) {
			System.out.println("* Considering var: " + var);
			System.out.println("  update time list: " + varUpdates.get(var).toString());
			// Associate with each var the corresponding update streams of its
			// final nodes.
			Set<List<Long>> assocFinalNodeUpdateTimesForVar = new HashSet<>();
			for (String n : finalNodesOfVars.get(var)) {
				System.out.println("  -> Considering final node: " + n);
				List<Update> us = this.finalNodeUpdates.get(n).stream()
						.filter(u -> u.getSource().equals(var + "@" + graph.getHost(var))).collect(Collectors.toList());
				System.out.println("     update list: " + us.toString());
				List<Long> ts = us.stream().map(u -> u.getTime()).collect(Collectors.toList());
				System.out.println("     update time list: " + ts.toString());
				assocFinalNodeUpdateTimesForVar.add(ts);
			}
			System.out.println(assocFinalNodeUpdateTimesForVar.toString());

			// Calculate the total propagation delay for each var update for
			// each var.
			List<Long> varUpdatesForVar = varUpdates.get(var);
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
