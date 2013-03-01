package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.Fixtures

class StudentRelationshipTest extends TestBase with Mockito {
	
	val profileService = mock[ProfileService]
	
	@Test def agentMember {
		val rel = StudentRelationship("0672089", RelationshipType.PersonalTutor, "0205225/1")
		rel.profileService = profileService
		
		rel.isAgentMember should be (true)
		
		profileService.getMemberByUniversityId("0672089") returns (None)
		
		rel.agentMember should be (None)
		rel.agentParsed should be ("0672089")
		rel.agentName should be ("0672089")
		rel.agentLastName should be ("0672089")
		
		val staff = Fixtures.staff(universityId="0672089")
		staff.firstName = "Steve"
		staff.lastName = "Taff"
		
		profileService.getMemberByUniversityId("0672089") returns (Some(staff))
		
		rel.agentMember should be (Some(staff))
		rel.agentParsed should be (staff)
		rel.agentName should be ("Steve Taff")
		rel.agentLastName should be ("Taff")
		
		rel.studentId should be ("0205225")
		
		val student = Fixtures.student()
		profileService.getStudentBySprCode("0205225/1") returns (Some(student))
		
		rel.studentMember should be (Some(student))
	}
  
	@Test def convertToObject() {
		val t = new RelationshipUserType
		t.convertToObject("personalTutor") should be (RelationshipType.PersonalTutor)
		evaluating { t.convertToObject("Q") } should produce [IllegalArgumentException]
	}
  
	@Test def convertToValue() {
		val t = new RelationshipUserType
		t.convertToValue(RelationshipType.PersonalTutor) should be ("personalTutor")
	}

}