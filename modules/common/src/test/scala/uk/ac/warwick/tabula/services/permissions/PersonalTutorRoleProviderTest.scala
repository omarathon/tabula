package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.roles.PersonalTutor
import uk.ac.warwick.tabula.data.model.RelationshipType
import uk.ac.warwick.tabula.data.model.StudentRelationship

class PersonalTutorRoleProviderTest extends TestBase with Mockito {
	
	val provider = new PersonalTutorRoleProvider
	
	val profileService = mock[ProfileService]
	provider.profileService = profileService
	
	val member = Fixtures.student(universityId = "111111")
	
	@Test def isTutor = withUser("cuscav", "0123456") {
		val rel1 = StudentRelationship("0123456", RelationshipType.PersonalTutor, "111111/1")
		val rel2 = StudentRelationship("0123456", RelationshipType.PersonalTutor, "888888/1")
		
		profileService.listStudentRelationshipsWithUniversityId(RelationshipType.PersonalTutor, "0123456") returns (Seq(rel1, rel2))
		
		provider.getRolesFor(currentUser, member) should be (Seq(PersonalTutor(member)))
	}
	
	@Test def notTutor = withUser("cuscav", "0123456") {
		profileService.listStudentRelationshipsWithUniversityId(RelationshipType.PersonalTutor, "0123456") returns (Seq())
		
		provider.getRolesFor(currentUser, member) should be (Seq())
	}
	
	@Test def handlesDefault = withUser("cuscav", "0123456") {
		provider.getRolesFor(currentUser, Fixtures.department("in", "IN202")) should be (Seq())
	}

}