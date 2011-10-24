package uk.ac.warwick.courses.validators;

public final class TestValidJavaObject {
	
	@HasLetter(letter='e')
	private final String name;
	
	public TestValidJavaObject(String n) {
		name = n;
	}
	
	public String getName() {
		return name;
	}
}
