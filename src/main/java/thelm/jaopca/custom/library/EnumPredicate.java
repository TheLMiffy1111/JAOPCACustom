package thelm.jaopca.custom.library;

public enum EnumPredicate {

	LESS_THAN,
	GREATER_THAN,
	LESS_THAN_OR_EQUAL_TO,
	GREATER_THAN_OR_EQUAL_TO,
	TRUE,
	FALSE;
	
	public static EnumPredicate fromName(String name) {
		for(EnumPredicate predicate : EnumPredicate.values()) {
			if(predicate.name().equalsIgnoreCase(name)) {
				return predicate;
			}
		}
		return null;
	}
}
