package uk.ac.warwick.tabula.commands.profiles.profile

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, StudentMember}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.permissions.Permissions.UserPicker
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

object ViewProfileSubsetCommand {
  type Command = Appliable[ProfileSubset]

  def apply(student: User): Command =
    new ViewProfileSubsetCommandInternal(student)
      with ComposableCommand[ProfileSubset]
      with AutowiringProfileServiceComponent
      with ViewProfileSubsetCommandPermissions
      with Unaudited with ReadOnly {

      override val studentMember: Option[StudentMember] = student.getWarwickId.maybeText
        .map(uid => profileService.getMemberByUniversityId(uid))
        .collect { case Some(sm: StudentMember) => sm }

      // only try to get the user via lookup if no student member is found
      override val user: Option[User] = studentMember match {
        case Some(_) => None
        case None => Option(student).filter(_.isFoundUser)
      }

    }
}

abstract class ViewProfileSubsetCommandInternal(student: User)
  extends CommandInternal[ProfileSubset] with ViewProfileSubsetCommandState {

  override def applyInternal(): ProfileSubset =
    ProfileSubset(
      studentMember.nonEmpty,
      user,
      studentMember,
      studentMember.flatMap(_.mostSignificantCourseDetails)
    )
}

trait ViewProfileSubsetCommandState {
  val studentMember: Option[StudentMember]
  val user: Option[User]
}

trait ViewProfileSubsetCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ViewProfileSubsetCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    studentMember match {
      case Some(student) => p.PermissionCheck(Permissions.Profiles.Read.Core, mandatory(student))
      case _ => p.PermissionCheck(UserPicker)
    }
}

case class ProfileSubset(
  isMember: Boolean,
  user: Option[User],
  profile: Option[StudentMember],
  courseDetails: Option[StudentCourseDetails]
)
