package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.roles.StudentRelationshipAgent
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

class StudentRelationshipAgentRoleProviderTest extends TestBase with Mockito {

	val provider = new StudentRelationshipAgentRoleProvider

	val relationshipService = mock[RelationshipService]
	provider.relationshipService = relationshipService
	
	val profileService = mock[ProfileService]

	val member = Fixtures.student(universityId = "111111")

	@Test def isAgent = withUser("cuscav", "0123456") {
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		
		val rel1 = StudentRelationship(member, relationshipType, member)	
		val rel2 = StudentRelationship(member, relationshipType, member)

		relationshipService.listAllStudentRelationshipsWithUniversityId("0123456") returns (Seq(rel1, rel2))

		provider.getRolesFor(currentUser, member).force should be (Seq(StudentRelationshipAgent(member, relationshipType)))
	}

	@Test def notAgent = withUser("cuscav", "0123456") {
		relationshipService.listAllStudentRelationshipsWithUniversityId("0123456") returns (Seq())

		provider.getRolesFor(currentUser, member) should be (Seq())
	}

	@Test def handlesDefault = withUser("cuscav", "0123456") {
		provider.getRolesFor(currentUser, Fixtures.department("in", "IN202")) should be (Seq())
	}

}