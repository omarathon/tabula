package uk.ac.warwick.tabula.data.model.permissions

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.permissions.PermissionsSelector
import uk.ac.warwick.tabula.permissions.Permissions

class PermissionUserTypeTest extends TestBase with Mockito {

	@Test def convertToObject() {
		val t = new PermissionUserType
		t.convertToObject("Department.Create") should be (Permissions.Department.Create)
		t.convertToObject("Profiles.Read.Core") should be (Permissions.Profiles.Read.Core)
		t.convertToObject("Masquerade") should be (Permissions.Masquerade)
		evaluating { t.convertToObject("Q") } should produce [IllegalArgumentException]
	}

	@Test def convertToValue() {
		val t = new PermissionUserType
		t.convertToValue(Permissions.Department.Create) should be ("Department.Create")
		t.convertToValue(Permissions.Profiles.Read.Core) should be ("Profiles.Read.Core")
		t.convertToValue(Permissions.Masquerade) should be ("Masquerade")
	}

	@Test def selectorRoles() {
		val t = new PermissionUserType

		val tutorType = StudentRelationshipType("personalTutor", "tutor", "tutor", "tutee")
		t.relationshipService.set(mock[RelationshipService])
		t.relationshipService.get.getStudentRelationshipTypeById("personalTutor") returns (Some(tutorType))

		t.convertToObject("Profiles.MeetingRecord.ReadDetails(*)") should be (Permissions.Profiles.MeetingRecord.ReadDetails(PermissionsSelector.Any[StudentRelationshipType]))
		t.convertToObject("Profiles.MeetingRecord.ReadDetails(personalTutor)") should be (Permissions.Profiles.MeetingRecord.ReadDetails(tutorType))

		t.convertToValue(Permissions.Profiles.MeetingRecord.ReadDetails(PermissionsSelector.Any[StudentRelationshipType])) should be ("Profiles.MeetingRecord.ReadDetails(*)")
		t.convertToValue(Permissions.Profiles.MeetingRecord.ReadDetails(tutorType)) should be ("Profiles.MeetingRecord.ReadDetails(personalTutor)")
	}

}