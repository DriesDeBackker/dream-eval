package evalapp.master;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import dream.client.UpdateProducer;
import dream.client.Var;
import evalapp.commands.Command;
import evalapp.commands.CommandsGenerator;
import evalapp.commands.IterationSpecifics;
import evalapp.graphgenerator.Graph;
import evalapp.graphgenerator.GraphGenerator;
import evalapp.valgenerator.RandomNormalInteger;
import evalapp.valgenerator.ValueGenerator;

public class ProgramDeployer extends Client {
	private static final String HOSTNAME = "master";
	private static final String[] clients = { "host1", "host2", "host3", "host4", "host5" };

	private Var<Command> cmdsVar;
	private GraphGenerator gg;
	private CommandsGenerator<Integer> cg;
	private IterationSpecifics is;
	private ValueGenerator<Integer> vg;
	private List<Command> cmds;

	public ProgramDeployer() {
		super(HOSTNAME);
		deployProgram(cmds);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void init() {
		this.cmdsVar = new Var<Command>("commands", null);
		this.is = new IterationSpecifics(Config.update_interval_mean, Config.update_interval_sd);
		this.gg = new GraphGenerator(Config.random_seed);
		gg.generateGraph();
		Graph graph = gg.getGraph();
		this.vg = new RandomNormalInteger(Config.values_mean, Config.values_sd);
		List<Function<UpdateProducer<Integer>, ?>> fns = new ArrayList<Function<UpdateProducer<Integer>, ?>>();
		Function<UpdateProducer<Integer>, Integer> unary = (Function<UpdateProducer<Integer>, Integer> & Serializable) a -> a
				.get();
		fns.add(unary);
		Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Integer>> binary = (Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Integer>> & Serializable) a -> b -> Math
				.round((a.get() + b.get()) / 2);
		fns.add(binary);
		Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Integer>>> ternary = (Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Integer>>> & Serializable) a -> b -> c -> Math
				.round((a.get() + b.get() + c.get()) / 3);
		fns.add(ternary);
		Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Integer>>>> quaternary = (Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Function<UpdateProducer<Integer>, Integer>>>> & Serializable) a -> b -> c -> d -> Math
				.round((a.get() + b.get() + c.get()) + d.get() / 4);
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
			varsToWaitFor.add("ready@" + clients[i]);
		}
		return varsToWaitFor;
	}

	private void deployProgram(List<Command> cmds) {
		System.out.println("Deploying distributed reactive program");
		for (Command cmd : cmds) {
			cmdsVar.set(cmd);
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
