package evalapp.master;

import java.io.Serializable;

public class Update implements Serializable {

	private static final long serialVersionUID = -2767622289492784254L;
	private Long time;
	private String source;

	public Update(String source, Long time) {
		this.source = source;
		this.time = time;
	}

	public String getSource() {
		return source;
	}

	public Long getTime() {
		return time;
	}

}
