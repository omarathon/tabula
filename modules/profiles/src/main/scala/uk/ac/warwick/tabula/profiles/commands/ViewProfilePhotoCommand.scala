package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.commands.profiles.{MemberPhotoUrlGeneratorComponent, PhotosWarwickMemberPhotoUrlGeneratorComponent, ServesPhotosFromExternalApplication}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.web.Mav

object ViewProfilePhotoCommand {
	def apply(member: Member) = new ViewProfilePhotoCommand(member) with PhotosWarwickMemberPhotoUrlGeneratorComponent
}

abstract class ViewProfilePhotoCommand(val member: Member)
	extends Command[Mav] with ReadOnly with Unaudited with ServesPhotosFromExternalApplication {

	this: MemberPhotoUrlGeneratorComponent =>

	PermissionCheck(Permissions.Profiles.Read.Core, mandatory(member))

	override def applyInternal() = {
		Mav(s"redirect:${photoUrl(Option(member))}")
	}

	override def describe(d: Description) = d.member(member)
}

class ViewStudentRelationshipPhotoCommand(val member: Member, val relationship: StudentRelationship)
	extends Command[Mav] with ReadOnly with Unaudited with ServesPhotosFromExternalApplication with PhotosWarwickMemberPhotoUrlGeneratorComponent {

	PermissionCheck(Permissions.Profiles.StudentRelationship.Read(relationship.relationshipType), member)

	override def applyInternal() = {
		Mav(s"redirect:${photoUrl(relationship.agentMember)}")
	}

	override def describe(d: Description) = d.member(member).property("relationship" -> relationship)

}