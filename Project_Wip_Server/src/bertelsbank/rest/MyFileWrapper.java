package bertelsbank.rest;

import java.io.File;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MyFileWrapper {
	File[] files;

	public MyFileWrapper(File[] files) {
		this.files = files;
	}

	//Default Konstruktor
	public MyFileWrapper(){}

	public File[] getFiles() {
		return files;
	}

	public void setFiles(File[] files) {
		this.files = files;
	}

}
