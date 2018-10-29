package bertelsbank.rest;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RestData {
	private String info;

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

}
