package uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.services.RelationshipServiceImpl

class StudentCourseDetailsTest extends PersistenceTestBase with Mockito {

	val profileService = mock[ProfileService]
	val relationshipService = mock[RelationshipService]

	@Test def getPersonalTutor {
		val student = new StudentMember

		val studentCourseDetails = new StudentCourseDetails(student, "0205225/1")
		studentCourseDetails.sprCode = "0205225/1"
		studentCourseDetails.relationshipService = relationshipService

		student.studentCourseDetails.add(studentCourseDetails)
		student.profileService = profileService
		
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

		profileService.getStudentBySprCode("0205225/1") returns (Some(student))

		relationshipService.findCurrentRelationships(relationshipType, "0205225/1") returns (Nil)
		student.studentCourseDetails.get(0).relationships(relationshipType) should be ('empty)

		val rel = StudentRelationship("0672089", relationshipType, "0205225/1")
		rel.profileService = profileService

		relationshipService.findCurrentRelationships(relationshipType, "0205225/1") returns (Seq(rel))
		profileService.getMemberByUniversityId("0672089") returns (None)
		student.studentCourseDetails.get(0).relationships(relationshipType) map { _.agentParsed } should be (Seq("0672089"))

		val staff = Fixtures.staff(universityId="0672089")
		staff.firstName = "Steve"
		staff.lastName = "Taff"

		profileService.getMemberByUniversityId("0672089") returns (Some(staff))

		student.studentCourseDetails.get(0).relationships(relationshipType) map { _.agentParsed } should be (Seq(staff))
	}

}