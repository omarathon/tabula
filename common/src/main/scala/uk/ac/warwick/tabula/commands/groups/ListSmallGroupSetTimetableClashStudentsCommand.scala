package uk.ac.warwick.tabula.commands.groups

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.ListSmallGroupSetTimetableClashStudentsCommand._
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}

import scala.collection.JavaConverters._
import scala.collection.SortedMap

object ListSmallGroupSetTimetableClashStudentsCommand {
	type Result = SortedMap[SmallGroup, Seq[Member]]
	type Command = Appliable[Result] with ListSmallGroupSetTimetableClashStudentsCommandState
	
	def apply(smallGroupSet: SmallGroupSet): Command = {
		new ListSmallGroupSetTimetableClashStudentsCommandInternal(smallGroupSet)
			with ComposableCommand[Result]
			with AutowiringProfileServiceComponent
			with ListSmallGroupSetTimetableClashStudentsCommandPermissions
			with ListSmallGroupSetTimetableClashStudentsCommandState
			with Unaudited with ReadOnly
	}
}

class ListSmallGroupSetTimetableClashStudentsCommandInternal(val smallGroupSet: SmallGroupSet)
	extends CommandInternal[Result] with ListSmallGroupSetTimetableClashStudentsCommandState {
	self: ProfileServiceComponent =>

	override def applyInternal(): Result =
		SortedMap(
			clashStudents.asScala
				.filter { case (group, _) => smallGroupSet.groups.contains(group) }
				.map { case (group, usercodes) =>
					group ->
						smallGroupSet.allStudents
							.filter { user => usercodes.contains(user.getUserId) } // ensure users from this group are only displayed
							.flatMap { user => profileService.getAllMembersWithUserId(user.getUserId) }
							.distinct
				}
				.filter { case (_, students) => students.nonEmpty }
				.toSeq: _*
		)
}

trait ListSmallGroupSetTimetableClashStudentsCommandState {
	val smallGroupSet: SmallGroupSet
	var clashStudents: JMap[SmallGroup, Array[String]] = JHashMap()
}

trait ListSmallGroupSetTimetableClashStudentsCommandPermissions extends RequiresPermissionsChecking {
	self: ListSmallGroupSetTimetableClashStudentsCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.ReadMembership, smallGroupSet)
	}
}