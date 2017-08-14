package thelm.jaopca.custom.library;

public enum EnumNumberFunction {

	CONSTANT,
	POLYNOMIAL,
	POWER,
	EXPONENTIAL;

	public static EnumNumberFunction fromName(String name) {
		for(EnumNumberFunction function : EnumNumberFunction.values()) {
			if(function.name().equalsIgnoreCase(name)) {
				return function;
			}
		}
		return null;
	}
}
