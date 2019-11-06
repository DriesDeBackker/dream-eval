package evalapp.commands;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import dream.client.UpdateProducer;
import evalapp.graphgenerator.DependencyGraph;
import evalapp.valgenerator.ValueGenerator;

public class CommandsGenerator<T extends Serializable> {

	private List<Command> commands;
	private DependencyGraph graph;
	private ValueGenerator<T> vg;
	private IterationSpecifics is;
	private List<Function<UpdateProducer<T>, ?>> fns;
	private Experiment experiment;

	public CommandsGenerator(Experiment exp, DependencyGraph graph, ValueGenerator<T> vg, IterationSpecifics is,
			List<Function<UpdateProducer<T>, ?>> fns) {
		this.experiment = exp;
		this.commands = new ArrayList<Command>();
		this.graph = graph;
		this.vg = vg;
		this.is = is;
		this.fns = fns;
	}

	public void generateCommands() {
		generateVarCommands();
		generateMainCommands();
		if (experiment.equals(Experiment.DELAY)) {
			System.out.println("generating final commands");
			generateFinalCommands();
		}
	}

	private void generateVarCommands() {
		this.commands = new ArrayList<Command>();
		// Generate Var commands for the first graph level.
		Map<String, String> varLocationsByName = graph.getVars();
		for (String var : varLocationsByName.keySet()) {
			commands.add(new VarCommand<T>(varLocationsByName.get(var), var, vg.next(), vg, is));
		}
	}

	private void generateMainCommands() {
		// Generate commands for the other graph levels.
		for (int l = 2; l <= graph.getNumberOfLevels(); l++) {
			List<String> level = graph.getNodesAtLevel(l);
			for (String sig : level) {
				String sigHost = graph.getHost(sig);
				List<String> deps = graph.getSignalDeps(sig);
				for (String dep : deps) {
					String depHost = graph.getHost(dep);
					if (!depHost.equals(sigHost)) {
						commands.add(new RemoteVarCommand(sigHost, depHost, dep));
					}
				}
				String[] args = deps.toArray(new String[deps.size()]);
				commands.add(new SignalCommand<T>(sigHost, sig, Boolean.FALSE, fns.get(deps.size() - 1), args));
			}
		}
	}

	private void generateFinalCommands() {
		Map<String, String> hostsForVars = graph.getVars();
		Map<String, Set<String>> finalsForVars = new HashMap<>();
		for (String var : hostsForVars.keySet()) {
			Set<String> finals = graph.getFinalNodesOf(var);
			finalsForVars.put(var, finals);
			for (String node : finals) {
				commands.add(new RemoteVarCommand(hostsForVars.get(var), graph.getHost(node), node));
				commands.add(new SignalCommand<T>(hostsForVars.get(var), var + node, Boolean.TRUE, fns.get(0), node));
			}
		}

	}

	public List<Command> getCommands() {
		return commands;
	}

	public void setGraph(DependencyGraph graph) {
		this.graph = graph;
	}
}
