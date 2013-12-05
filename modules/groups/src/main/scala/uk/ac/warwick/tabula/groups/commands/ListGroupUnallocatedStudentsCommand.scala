package uk.ac.warwick.tabula.groups.commands

import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent, ProfileService}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.commands.ReadOnly
import scala.collection.JavaConverters._

import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.commands.MemberOrUser
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.CurrentUser


case class UnallocatedStudentsInformation (
	smallGroupSet: SmallGroupSet,
	studentsNotInGroups: Seq[MemberOrUser]
)

object ListGroupUnallocatedStudentsCommand {
	def apply(smallGroupSet: SmallGroupSet, user: CurrentUser) = {
		new ListGroupUnallocatedStudentsCommandInternal(smallGroupSet, user)
			with ComposableCommand[UnallocatedStudentsInformation]
			with ListGroupUnallocatedStudentsCommandPermissions
			with AutowiringProfileServiceComponent
			with Unaudited with ReadOnly
	}
}

class ListGroupUnallocatedStudentsCommandInternal(val smallGroupSet: SmallGroupSet, val user: CurrentUser)
	extends CommandInternal[UnallocatedStudentsInformation] with ListGroupUnallocatedStudentsCommandState  {

	  self:ProfileServiceComponent =>

			override def applyInternal() = {
				val studentsInGroups = smallGroupSet.groups.asScala.flatMap(_.students.users)
				val studentsNotInGroups = (smallGroupSet.allStudents diff studentsInGroups) map { user =>
					val member = profileService.getMemberByUniversityId(user.getWarwickId)
					MemberOrUser(member, user)
				}

				UnallocatedStudentsInformation(smallGroupSet, studentsNotInGroups)
			}
}

trait ListGroupUnallocatedStudentsCommandState {
	val user: CurrentUser
	val smallGroupSet: SmallGroupSet
}

trait ListGroupUnallocatedStudentsCommandPermissions extends RequiresPermissionsChecking {
	self:ListGroupUnallocatedStudentsCommandState =>
		def permissionsCheck(p: PermissionsChecking) {
			p.PermissionCheck(Permissions.SmallGroups.ReadMembership, smallGroupSet)
		}
}