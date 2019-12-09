package evalapp.master;

import java.io.Serializable;

public class Update implements Serializable {

	private static final long serialVersionUID = -2767622289492784254L;
	private final String source;
	private final Long sourceTime;
	private final Long endTime;

	public Update(String source, Long sourceTime, long endTime) {
		this.source = source;
		this.sourceTime = sourceTime;
		this.endTime = endTime;
	}

	public String getSource() {
		return source;
	}

	public Long getSourceTime() {
		return sourceTime;
	}

	public Long getEndTime() {
		return endTime;
	}

}
