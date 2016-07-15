package uk.ac.warwick.tabula.commands.profiles.profile

import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, StudentMember}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.permissions.Permissions.UserPicker
import uk.ac.warwick.tabula.services.{ProfileService, UserLookupService}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

object ViewProfileSubsetCommand {
	def apply(universityId: String, profileService: ProfileService, userLookup: UserLookupService) =
		new ViewProfileSubsetCommandInternal(universityId, profileService, userLookup)
			with ComposableCommand[ProfileSubset]
			with ViewProfileSubsetCommandPermissions
			with Unaudited
			with ReadOnly
}

abstract class ViewProfileSubsetCommandInternal(val universityId: String, profileService: ProfileService,	userLookup: UserLookupService)
	extends CommandInternal[ProfileSubset] with ViewProfileSubsetCommandState {

	val studentMember = profileService.getMemberByUniversityId(universityId).collect{ case sm:StudentMember => sm }

	// only try to get the user via lookup if no student member is found
	val user = studentMember match {
		case Some(_) => None
		case None => Option(userLookup.getUserByWarwickUniId(universityId))
	}

	def applyInternal() = {
		if (studentMember.isDefined || user.isDefined)
			ProfileSubset(studentMember.isDefined, user, studentMember, studentMember.flatMap(_.mostSignificantCourseDetails))
		else
			throw new ItemNotFoundException()
	}
}

trait ViewProfileSubsetCommandState {
	val universityId: String
	val studentMember: Option[StudentMember]
	val user: Option[User]
}

trait ViewProfileSubsetCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ViewProfileSubsetCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		studentMember.foreach(p.PermissionCheck(Permissions.Profiles.Read.Core, _))
		if (user.isDefined) p.PermissionCheck(UserPicker)
	}
}

case class ProfileSubset (
	isMember: Boolean,
	user: Option[User],
	profile: Option[StudentMember],
	courseDetails: Option[StudentCourseDetails]
)