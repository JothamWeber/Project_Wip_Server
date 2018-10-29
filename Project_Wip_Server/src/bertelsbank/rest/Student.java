package bertelsbank.rest;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement //Annotation nur bei List-Wrapper Lösung notwendig
public class Student {
	private int id;
	private String name;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
