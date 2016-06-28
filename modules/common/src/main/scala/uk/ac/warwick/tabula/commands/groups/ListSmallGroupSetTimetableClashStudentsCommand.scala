package uk.ac.warwick.tabula.commands.groups

import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}


object ListSmallGroupSetTimetableClashStudentsCommand {
	def apply(smallGroupSet: SmallGroupSet, clashStudentUserIds: Seq[String]) = {
		new ListSmallGroupSetTimetableClashStudentsCommandInternal(smallGroupSet, clashStudentUserIds)
			with ComposableCommand[Seq[Member]]
			with AutowiringProfileServiceComponent
			with ListSmallGroupSetTimetableClashStudentsCommandPermissions
			with Unaudited with ReadOnly
	}
}

class ListSmallGroupSetTimetableClashStudentsCommandInternal(val smallGroupSet: SmallGroupSet, clashStudentUserIds: Seq[String])
	extends CommandInternal[Seq[Member]] with ListSmallGroupSetTimetableClashStudentsCommandState {
	self: ProfileServiceComponent =>

	override def applyInternal() = {
		//ensure users from this group are only displayed
		val users = smallGroupSet.allStudents.filter { user =>  clashStudentUserIds.contains(user.getUserId) }
		users.flatMap { user => profileService.getAllMembersWithUserId(user.getUserId) }.distinct
	}
}

trait ListSmallGroupSetTimetableClashStudentsCommandState {
	val smallGroupSet: SmallGroupSet
}

trait ListSmallGroupSetTimetableClashStudentsCommandPermissions extends RequiresPermissionsChecking {
	self: ListSmallGroupSetTimetableClashStudentsCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.ReadMembership, smallGroupSet)
	}
}