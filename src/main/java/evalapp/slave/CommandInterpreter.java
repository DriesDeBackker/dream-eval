package evalapp.slave;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

import dream.client.DreamClient;
import dream.client.RemoteVar;
import dream.client.Signal;
import dream.client.UpdateProducer;
import dream.client.Var;
import dream.common.Consts;
import evalapp.commands.Command;
import evalapp.commands.EndCommand;
import evalapp.commands.IterationSpecifics;
import evalapp.commands.RemoteVarCommand;
import evalapp.commands.SignalCommand;
import evalapp.commands.StartCommand;
import evalapp.commands.VarCommand;
import evalapp.master.Update;
import evalapp.valgenerator.ValueGenerator;

public class CommandInterpreter {
	private String hostname;

	protected Logger logger;

	private Map<String, UpdateProducer<?>> updateProducers = new HashMap<>();
	private List<Var<?>> vars = new ArrayList<>();
	private List<Signal<?>> finalNodes = new ArrayList<>();

	private List<String> varsToWaitFor;
	private boolean running = false;

	private Var<HashMap<String, List<Long>>> varUpdates;
	private Var<HashMap<String, List<Update>>> finalNodeUpdates;

	public CommandInterpreter(String host) {
		this.hostname = host;
		Consts.hostName = host;

		logger = Logger.getLogger(host);

		DreamClient.instance.connect();

		addInitialVarsToWaitFor();

		waitForVars();

		RemoteVar<Command> cs = new RemoteVar<Command>("master", "commands");
		new Signal<Boolean>("processcommands", () -> {
			System.out.println("received command.");
			boolean deployed = process(cs.get());
			System.out.println(deployed);
			return deployed;
		}, cs);

		this.varUpdates = new Var<HashMap<String, List<Long>>>("varUpdates", null);
		this.finalNodeUpdates = new Var<HashMap<String, List<Update>>>("finalNodeUpdates", null);

		System.out.println("Client initialization finished.");
		logger.fine("Client initialization finished.");
	}

	private void waitForVars() {
		// wait for vars needed by the client
		try {
			if (!this.varsToWaitFor.isEmpty())
				System.out.println("Waiting for Vars: " + this.varsToWaitFor.toString());
			logger.fine("Waiting for Vars: " + this.varsToWaitFor.toString());
			while (!allVarsAvailable()) {
				Thread.sleep(500);
			}
			assert (allVarsAvailable());
			System.out.println("Vars are now all available.");
			logger.fine("Vars are now all available.");
			this.varsToWaitFor.clear();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("rawtypes")
	private boolean process(Command command) {
		if (command == null || !command.getTarget().equals(this.hostname)) {
			return false;
		}
		if (command instanceof StartCommand) {
			System.out.println("Experiment started.");
			this.processStartCommand();
		} else if (command instanceof EndCommand) {
			System.out.println("Experiment finished. Sending results...");
			this.processEndCommand();
		} else if (command instanceof VarCommand<?>) {
			System.out.println("Deploying a new Var.");
			logger.fine("Deploying a new Var.");
			this.processVarCommand((VarCommand<?>) command);
		} else if (command instanceof RemoteVarCommand) {
			System.out.println("Deploying a new RemoteVar.");
			logger.fine("Deploying a new RemoteVar.");
			this.processRemoteVarCommand((RemoteVarCommand) command);
		} else if (command instanceof SignalCommand) {
			System.out.println("Deploying a new Signal.");
			logger.fine("Deploying a new Signal.");
			this.processSignalCommand((SignalCommand) command);
		}
		return true;
	}

	private void processStartCommand() {
		this.running = true;
	}

	private void processEndCommand() {
		this.running = false;
		HashMap<String, List<Long>> varUpdates = new HashMap<String, List<Long>>();
		for (Var<?> v : vars) {
			varUpdates.put(v.getObject(), v.getUpdateLog());
		}
		this.varUpdates.set(varUpdates);
		HashMap<String, List<Update>> finalNodeUpdates = new HashMap<>();
		for (Signal<?> s : finalNodes) {
			finalNodeUpdates.put(s.getObject(), s.getUpdateLog());
		}
		this.finalNodeUpdates.set(finalNodeUpdates);
	}

	private void processVarCommand(VarCommand<?> command) {
		Var<?> newVar = new Var<>(command.getName(), command.getInitialValue());
		this.addUpdateProducer(command.getName(), newVar);
		this.vars.add(newVar);

		Thread t1 = new Thread(new Runnable() {
			public void run() {
				IterationSpecifics is = command.getIs();
				long mean = is.getMean();
				long sd = is.getSd();
				ValueGenerator<?> g = command.getGenerator();
				Random r = new Random();
				while (true) {
					if (running) {
						newVar.setUnsafe(g.next());
					}
					double next = r.nextGaussian() * sd + mean;
					try {
						Thread.sleep((long) next);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		t1.start();
	}

	private void processRemoteVarCommand(RemoteVarCommand command) {
		this.varsToWaitFor.add(command.getRemvar() + "@" + command.getRemhost());
		this.waitForVars();
		RemoteVar<? extends Serializable> newRemVar = new RemoteVar<>(command.getRemhost(), command.getRemvar());
		this.addUpdateProducer(command.getRemvar(), newRemVar);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void processSignalCommand(SignalCommand command) {
		UpdateProducer<?>[] args = Arrays.stream(command.getArgs()).parallel()
				.map(name -> this.updateProducers.get(name)).sequential().toArray(UpdateProducer<?>[]::new);
		Function<UpdateProducer<?>, ?> fn = command.getFn();
		int nbArgs = args.length;
		for (int i = 0; i < nbArgs - 1; i++) {
			fn = (Function<UpdateProducer<?>, ?>) fn.apply(args[i]);
		}
		final Function<UpdateProducer<?>, ?> fnend = fn;
		Supplier<?> closure = () -> fnend.apply(args[nbArgs - 1]);
		Signal<?> newSignal = new Signal<>(command.getName(), (Supplier<? extends Serializable>) closure, args);
		this.addUpdateProducer(command.getName(), newSignal);
		if (command.isFinalNode()) {
			this.finalNodes.add(newSignal);
		}
	}

	private void addInitialVarsToWaitFor() {
		this.varsToWaitFor = new ArrayList<String>();
		this.varsToWaitFor.add("commands@master");
	}

	private void addUpdateProducer(String name, UpdateProducer<?> rv) {
		this.updateProducers.put(name, rv);
	}

	private boolean allVarsAvailable() {
		for (String var : this.varsToWaitFor) {
			if (!DreamClient.instance.listVariables().contains(var))
				return false;
		}
		return true;
	}

}
