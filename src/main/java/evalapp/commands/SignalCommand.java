package evalapp.commands;

import java.io.Serializable;
import java.util.function.Function;

import dream.client.UpdateProducer;

public class SignalCommand<T extends Serializable> extends Command implements Serializable {

	private static final long serialVersionUID = -5516875372186775573L;

	private String[] args;
	private Function<UpdateProducer<T>, ?> fn;
	private String name;

	public SignalCommand(String target, String name, Function<UpdateProducer<T>, ?> fn, String... args) {
		super(target);
		this.name = name;
		this.fn = fn;
		this.args = args;
	}

	/*
	 * public SignalCommand(String target, String name,
	 * Function<UpdateProducer<?>, Function<UpdateProducer<?>, ?>> fn, String
	 * arg1, String arg2) { super(target); this.fn = fn; }
	 * 
	 * public SignalCommand(String target, String name,
	 * Function<UpdateProducer<?>, Function<UpdateProducer<?>,
	 * Function<UpdateProducer<?>, ?>>> fn, String arg1, String arg2, String
	 * arg3) { super(target); this.fn = fn; }
	 */

	public String getName() {
		return name;
	}

	public Function<UpdateProducer<T>, ?> getFn() {
		return fn;
	}

	public String[] getArgs() {
		return args;
	}

}
