package evalapp.commands;

import java.io.Serializable;

public class VarCommand extends Command implements Serializable {

	private static final long serialVersionUID = 8886245125293165876L;
	private String name;
	private Serializable initialValue;

	public VarCommand(String target, String name, Serializable initialValue) {
		super(target);
		this.setName(name);
		this.setInitialValue(initialValue);
	}

	public String getName() {
		return name;
	}

	private void setName(String name) {
		this.name = name;
	}

	public Serializable getInitialValue() {
		return initialValue;
	}

	private void setInitialValue(Serializable initialValue) {
		this.initialValue = initialValue;
	}

}
