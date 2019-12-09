package evalapp.commands;

import java.io.Serializable;

public class IterationSpecifics implements Serializable {

	private static final long serialVersionUID = 5608013629817196353L;
	private long mean;
	private long sd;

	public IterationSpecifics(long mean, long sd) {
		this.setMean(mean);
		this.setSd(sd);
	}

	public long getMean() {
		return mean;
	}

	private void setMean(long mean) {
		this.mean = mean;
	}

	public long getSd() {
		return sd;
	}

	private void setSd(long sd) {
		this.sd = sd;
	}

}
