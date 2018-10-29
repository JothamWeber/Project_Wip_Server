package bertelsbank.rest;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

//Nur für Lösung mit Liste notwendig
@XmlRootElement
public class MyStudentListWrapper {
	List<Student> students;

	public List<Student> getStudents() {
		return students;
	}

	public void setStudents(List<Student> students) {
		this.students = students;
	}

	public MyStudentListWrapper(List<Student> students) {
		this.students = students;
	}

	public MyStudentListWrapper() {}

}
