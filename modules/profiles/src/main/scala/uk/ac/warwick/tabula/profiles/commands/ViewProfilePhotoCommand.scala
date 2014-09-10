package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.commands.profiles.ResizesPhoto
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands.ApplyWithCallback
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model.StudentRelationship

class ViewProfilePhotoCommand(val member: Member)
	extends Command[RenderableFile] with ReadOnly with ApplyWithCallback[RenderableFile] with Unaudited with ResizesPhoto {

	PermissionCheck(Permissions.Profiles.Read.Core, mandatory(member))

	override def applyInternal() = {
		val renderable = render(Option(member))

		if (callback != null) callback(renderable)
		renderable
	}

	override def describe(d: Description) = d.member(member)

}

class ViewStudentRelationshipPhotoCommand(val member: Member, val relationship: StudentRelationship)
	extends Command[RenderableFile] with ReadOnly with ApplyWithCallback[RenderableFile] with Unaudited  with ResizesPhoto {

	PermissionCheck(Permissions.Profiles.StudentRelationship.Read(relationship.relationshipType), member)

	override def applyInternal() = {
		val attachment = render(relationship.agentMember)

		if (callback != null) callback(attachment)

		attachment
	}

	override def describe(d: Description) = d.member(member).property("relationship" -> relationship)

}