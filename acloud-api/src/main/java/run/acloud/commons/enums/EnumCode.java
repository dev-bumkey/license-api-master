package run.acloud.commons.enums;

import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

public interface EnumCode {
	String getCode();
	
	static <E extends Enum<E> & EnumCode> E codeOf(Class<E> enumCodeClass, String code) throws Exception {
		for (E e : enumCodeClass.getEnumConstants()) {
			if (e.getCode().equals(code)) {
				return e;
			}
		}
		throw new CocktailException(String.format("No enum const %s for code [%s]", EnumCode.class, code),
                ExceptionType.InternalError);
	}
}
