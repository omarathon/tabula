package uk.ac.warwick.tabula.groups.commands

import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.commands.{MemberOrUser, Unaudited, ReadOnly, CommandInternal, ComposableCommand}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}

case class UnallocatedStudentsInformation (
	smallGroupSet: SmallGroupSet,
	membersNotInGroups: Seq[MemberOrUser],
	userIsMember: Boolean,
  showTutors: Boolean
)

object ListSmallGroupSetUnallocatedStudentsCommand {
	def apply(smallGroupSet: SmallGroupSet, user: CurrentUser) = {
		new ListSmallGroupSetUnallocatedStudentsCommandInternal(smallGroupSet, user)
			with ComposableCommand[UnallocatedStudentsInformation]
			with AutowiringProfileServiceComponent
			with ListSmallGroupSetUnallocatedStudentsCommandPermissions
			with Unaudited with ReadOnly
	}
}

class ListSmallGroupSetUnallocatedStudentsCommandInternal(val smallGroupSet: SmallGroupSet, val user: CurrentUser)
	extends CommandInternal[UnallocatedStudentsInformation] with ListSmallGroupSetUnallocatedStudentsCommandState  {
	self:ProfileServiceComponent =>

		override def applyInternal() = {
				val studentsNotInGroups = smallGroupSet.unallocatedStudents
				val userIsMember = studentsNotInGroups.exists(_.getWarwickId == user.universityId)
				val showTutors = smallGroupSet.studentsCanSeeTutorName

				val membersNotInGroups = studentsNotInGroups map { user =>
						val member = profileService.getMemberByUniversityId(user.getWarwickId)
						MemberOrUser(member, user)
				}

				UnallocatedStudentsInformation(smallGroupSet, membersNotInGroups, userIsMember, showTutors)
			}
}

trait ListSmallGroupSetUnallocatedStudentsCommandState {
	val user: CurrentUser
	val smallGroupSet: SmallGroupSet
}

trait ListSmallGroupSetUnallocatedStudentsCommandPermissions extends RequiresPermissionsChecking {
	self:ListSmallGroupSetUnallocatedStudentsCommandState =>
		def permissionsCheck(p: PermissionsChecking) {
			p.PermissionCheck(Permissions.SmallGroups.ReadMembership, smallGroupSet)
		}
}