package evalapp.commands;

import java.io.Serializable;

public class RemoteVarCommand extends Command implements Serializable {

	private static final long serialVersionUID = 627991636938494877L;
	private String remhost;
	private String remvar;

	public RemoteVarCommand(String target, String remhost, String remvar) {
		super(target);
		this.setRemhost(remhost);
		this.setRemvar(remvar);
	}

	public String getRemvar() {
		return remvar;
	}

	private void setRemvar(String remvar) {
		this.remvar = remvar;
	}

	public String getRemhost() {
		return remhost;
	}

	private void setRemhost(String remhost) {
		this.remhost = remhost;
	}

}
