package de.fhdw.server.example.rest;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ResultData {
	double result;

	public double getResult() {
		return result;
	}

	public void setResult(double result) {
		this.result = result;
	}

}
