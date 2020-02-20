package uk.ac.warwick.tabula.commands.profiles.profile

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, StudentMember}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringSecurityServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.userlookup.User

object ViewProfileSubsetCommand {
  type Command = Appliable[ProfileSubset]

  def apply(student: User, viewer: CurrentUser): Command =
    new ViewProfileSubsetCommandInternal(student, viewer)
      with ViewProfilePermissions
      with AutowiringProfileServiceComponent
      with AutowiringSecurityServiceComponent
      with ComposableCommand[ProfileSubset] // Init late, PermissionsChecking needs the autowired services
      with Unaudited with ReadOnly
}

abstract class ViewProfileSubsetCommandInternal(student: User, val viewer: CurrentUser)
  extends CommandInternal[ProfileSubset]
    with ViewProfileCommandState {
  self: ProfileServiceComponent =>

  override lazy val profile: MemberOrUser = MemberOrUser(
    member = student.getWarwickId.maybeText
      .flatMap(profileService.getMemberByUniversityId(_))
      .collect { case sm: StudentMember => sm },
    user = student
  )

  override def applyInternal(): ProfileSubset = {
    val studentMember: Option[StudentMember] = profile.asMember.collect { case sm: StudentMember => sm }

    ProfileSubset(
      profile.isMember,
      Option(profile.asUser).filter(_.isFoundUser),
      studentMember,
      studentMember.flatMap(_.mostSignificantCourseDetails)
    )
  }
}

case class ProfileSubset(
  isMember: Boolean,
  user: Option[User],
  profile: Option[StudentMember],
  courseDetails: Option[StudentCourseDetails]
)
