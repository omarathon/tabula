package uk.ac.warwick.tabula.commands.groups

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, MemberOrUser, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}



object ListSmallGroupSetTimetableClashStudentsCommand {
	def apply(smallGroupSet: SmallGroupSet, user: CurrentUser, clashStudentUserIds: Seq[String]) = {
		new ListSmallGroupSetTimetableClashStudentsCommandInternal(smallGroupSet, user, clashStudentUserIds)
			with ComposableCommand[Seq[MemberOrUser]]
			with AutowiringProfileServiceComponent
			with ListSmallGroupSetTimetableClashStudentsCommandPermissions
			with Unaudited with ReadOnly
	}
}

class ListSmallGroupSetTimetableClashStudentsCommandInternal(val smallGroupSet: SmallGroupSet, val user: CurrentUser, clashStudentUserIds: Seq[String])
	extends CommandInternal[Seq[MemberOrUser]] with ListSmallGroupSetTimetableClashStudentsCommandState {
	self: ProfileServiceComponent =>

	override def applyInternal() = {
		//ensure users from this group are only displayed
		val groupStudentds = smallGroupSet.allStudents
		val groupStudentUserIds = clashStudentUserIds.filter { userId => groupStudentds.exists {groupUser => userId == groupUser.getUserId }}
		groupStudentUserIds.flatMap { userId =>
			profileService.getAllMembersWithUserId(userId).map(member => MemberOrUser(member)) }.distinct
	}
}

trait ListSmallGroupSetTimetableClashStudentsCommandState {
	val user: CurrentUser
	val smallGroupSet: SmallGroupSet
}

trait ListSmallGroupSetTimetableClashStudentsCommandPermissions extends RequiresPermissionsChecking {
	self: ListSmallGroupSetTimetableClashStudentsCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.ReadMembership, smallGroupSet)
	}
}