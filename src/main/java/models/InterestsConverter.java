package models;

import java.util.LinkedList;
import java.util.List;

import org.neo4j.ogm.typeconversion.AttributeConverter;

public class InterestsConverter implements AttributeConverter<List<String>, String>{

	@Override
	public List<String> toEntityAttribute(String interests) {
		List<String> interestList = new LinkedList<>();
		for(String interest : interests.split(";"))
			interestList.add(interest);
		return interestList;
	}

	@Override
	public String toGraphProperty(List<String> interestList) {
		return String.join(";", interestList);
	}

}
