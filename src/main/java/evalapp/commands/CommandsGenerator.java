package evalapp.commands;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

	public CommandsGenerator(DependencyGraph graph, ValueGenerator<T> vg, IterationSpecifics is,
			List<Function<UpdateProducer<T>, ?>> fns) {
		this.commands = new ArrayList<Command>();
		this.graph = graph;
		this.vg = vg;
		this.is = is;
		this.fns = fns;
	}

	public void generateCommands() {
		this.commands = new ArrayList<Command>();
		// Generate Var commands for the first graph level.
		Map<String, String> varLocationsByName = graph.getVars();
		for (String var : varLocationsByName.keySet()) {
			commands.add(new VarCommand<T>(varLocationsByName.get(var), var, vg.next(), vg, is));
		}
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
				commands.add(new SignalCommand<T>(sigHost, sig, Boolean.valueOf(graph.isFinal(sig)),
						fns.get(deps.size() - 1), args));
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
