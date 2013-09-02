package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.permissions.PermissionsSelector

case class StudentRelationshipAgent(student: model.Member, relationshipType: StudentRelationshipType) extends BuiltInRole(StudentRelationshipAgentRoleDefinition(relationshipType), student)

case class StudentRelationshipAgentRoleDefinition(relationshipType: StudentRelationshipType) extends SelectorBuiltInRoleDefinition(relationshipType) {

	override def description = relationshipType.description

	GrantsScopedPermission(
		Profiles.Read.Core,
		Profiles.Read.NextOfKin,
		Profiles.Read.HomeAddress,
		Profiles.Read.TermTimeAddress,
		Profiles.Read.TelephoneNumber,
		Profiles.Read.MobileNumber,
		Profiles.Read.Usercode,

		Profiles.Read.StudentCourseDetails.Core,
		Profiles.Read.StudentCourseDetails.Status,
		Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]), // Can read any relationship type for this student

		Profiles.MeetingRecord.Create(relationshipType),
		Profiles.MeetingRecord.Read(relationshipType),
		Profiles.MeetingRecord.ReadDetails(relationshipType),
		Profiles.MeetingRecord.Update(relationshipType),
		Profiles.MeetingRecord.Delete(relationshipType),

		SmallGroups.Read,
		Profiles.Read.SmallGroups
	)
}
