package evalapp.master.experiment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

import dream.client.UpdateProducer;
import dream.client.Var;
import evalapp.commands.Command;
import evalapp.commands.CommandsGenerator;
import evalapp.commands.EndCommand;
import evalapp.commands.Experiment;
import evalapp.commands.IterationSpecifics;
import evalapp.commands.StartCommand;
import evalapp.commands.VarCommand;
import evalapp.graphgenerator.DependencyGraph;
import evalapp.graphgenerator.GraphGenerator;
import evalapp.graphgenerator.GraphVisualizer;
import evalapp.master.Client;
import evalapp.master.Config;
import evalapp.valgenerator.RandomNormalInteger;
import evalapp.valgenerator.ValueGenerator;

public abstract class ProgramDeployer extends Client {
	private static final String[] clients = { "host1", "host2", "host3", "host4", "host5" };

	private Var<Command> cmdsVar;
	private GraphGenerator gg;
	private CommandsGenerator<Integer> cg;
	private IterationSpecifics is;
	private ValueGenerator<Integer> vg;
	private List<Command> cmds;
	private Experiment exp;

	protected DependencyGraph graph;

	public ProgramDeployer(String hostname, Experiment exp) {
		super(hostname);
		setExperiment(exp);
	}

	@Override
	protected void init() {
		this.cmdsVar = new Var<Command>("commands", null);
		this.is = new IterationSpecifics(Config.update_interval_mean, Config.update_interval_sd);
		this.gg = new GraphGenerator(Config.random_seed);
		this.vg = new RandomNormalInteger(Config.values_mean, Config.values_sd);
	}

	public void run() {
		createProgram();
		deployProgram(cmds);
		prepareExperiment();
		askPermissionToStart();
		runExperiment();
		gatherResults();
		processResults();
	}

	@Override
	protected List<String> waitForVars() {
		List<String> varsToWaitFor = new ArrayList<String>();
		for (int i = 0; i < clients.length; i++) {
			varsToWaitFor.add("ready@" + clients[i]);
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

	}

	protected abstract void prepareExperiment();

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
				cmdsVar.set(new EndCommand("host" + i, exp));
				Thread.sleep(250);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected abstract void gatherResults();

	protected abstract void processResults();

	private void setExperiment(Experiment exp) {
		this.exp = exp;
	}
}
