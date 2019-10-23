package evalapp.commands;

import java.io.Serializable;

import evalapp.valgenerator.ValueGenerator;

public class VarCommand<T extends Serializable> extends Command implements Serializable {

	private static final long serialVersionUID = 8886245125293165876L;
	private String name;
	private T initialValue;
	private ValueGenerator<T> generator;
	private IterationSpecifics is;

	public VarCommand(String target, String name, T initialValue, ValueGenerator<T> generator, IterationSpecifics is) {
		super(target);
		this.setName(name);
		this.setInitialValue(initialValue);
		this.setGenerator(generator);
		this.setIs(is);
	}

	public String getName() {
		return name;
	}

	private void setName(String name) {
		this.name = name;
	}

	public T getInitialValue() {
		return initialValue;
	}

	private void setInitialValue(T initialValue) {
		this.initialValue = initialValue;
	}

	public ValueGenerator<T> getGenerator() {
		return this.generator;
	}

	private void setGenerator(ValueGenerator<T> generator) {
		this.generator = generator;
	}

	public IterationSpecifics getIs() {
		return is;
	}

	private void setIs(IterationSpecifics is) {
		this.is = is;
	}

}
