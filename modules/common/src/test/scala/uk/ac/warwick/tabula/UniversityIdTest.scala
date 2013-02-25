package uk.ac.warwick.tabula

class UniversityIdTest extends TestBase {
	
	@Test def itWorks() {
		UniversityId.isValid("0672089") should be (true)
		UniversityId.isValid("4072089") should be (true)
		UniversityId.isValid("672089") should be (false)
		UniversityId.isValid("06720890") should be (false)
		UniversityId.isValid("O672089") should be (false)
	}

}