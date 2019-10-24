package evalapp.master;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import dream.client.UpdateProducer;
import evalapp.commands.Command;
import evalapp.commands.IterationSpecifics;
import evalapp.commands.RemoteVarCommand;
import evalapp.commands.SignalCommand;
import evalapp.commands.VarCommand;
import evalapp.graphgenerator.Graph;
import evalapp.valgenerator.ValueGenerator;

public class CommandGenerator<T extends Serializable> {

	private List<Command> commands;
	private Graph graph;
	private ValueGenerator<T> vg;
	private IterationSpecifics is;
	private List<Function<UpdateProducer<?>, ?>> fns;

	public CommandGenerator(Graph graph, ValueGenerator<T> vg, IterationSpecifics is,
			List<Function<UpdateProducer<?>, ?>> fns) {
		this.setCommands(new ArrayList<Command>());
		this.setGraph(graph);
		this.setValueGenerator(vg);
		this.setIterationSpecifics(is);
		this.setFns(fns);
	}

	private void setFns(List<Function<UpdateProducer<?>, ?>> fns) {
		this.fns = fns;

	}

	private void setCommands(ArrayList<Command> commands) {
		this.commands = commands;
	}

	public void generateCommands() {
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
				String[] args = new String[deps.size()];
				args = deps.toArray(args);
				commands.add(new SignalCommand(sigHost, sig, fns.get(deps.size() - 1), args));
			}
		}

	}

	public List<Command> getCommands() {
		return commands;
	}

	private void setGraph(Graph graph) {
		this.graph = graph;
	}

	private void setValueGenerator(ValueGenerator<T> vg) {
		this.vg = vg;
	}

	private void setIterationSpecifics(IterationSpecifics is) {
		this.is = is;
	}

}
