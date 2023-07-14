package run.acloud.commons.enums;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum CRUDCommand implements EnumCode {
	C("Create")  // Create
	,R("Read") // Read
	,U("Update") // Update
	,D("Delete") // Delete
	,N("NoChange") // No Changes
	;

	@Getter
	private String value;

	CRUDCommand(String value) {
		this.value = value;
	}

	@Override
	public String getCode() {
		return this.name();
	}

	public static List<String> getCRUDListToString(){
		return Arrays.asList(CRUDCommand.values()).stream().map(s -> s.getCode()).collect(Collectors.toList());
	}

	public static List<CRUDCommand> getCRUDList(){
		return new ArrayList(Arrays.asList(CRUDCommand.values()));
	}

}
