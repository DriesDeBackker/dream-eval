package evalapp.commands;

import java.io.Serializable;

public abstract class Command implements Serializable {

	private static final long serialVersionUID = 6017711104208439237L;
	private String target;

	Command(String target) {
		this.setTarget(target);
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

}
