package uk.ac.warwick.tabula.commands.groups.admin.reusable

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroup, DepartmentSmallGroupSet, SmallGroup}
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._

object EditDepartmentSmallGroupsCommand {
	def apply(department: Department, set: DepartmentSmallGroupSet) =
		new EditDepartmentSmallGroupsCommandInternal(department, set)
			with ComposableCommand[Seq[DepartmentSmallGroup]]
			with EditDepartmentSmallGroupsPermissions
			with EditDepartmentSmallGroupsValidation
			with EditDepartmentSmallGroupsDescription
			with PopulateEditDepartmentSmallGroupsCommand
			with EditDepartmentSmallGroupsCommandRemoveTrailingEmptyGroups
			with AutowiringSmallGroupServiceComponent
}

trait EditDepartmentSmallGroupsCommandState {
	def department: Department
	def set: DepartmentSmallGroupSet

	var groupNames: JList[String] = LazyLists.create()
}

class EditDepartmentSmallGroupsCommandInternal(val department: Department, val set: DepartmentSmallGroupSet) extends CommandInternal[Seq[DepartmentSmallGroup]] with EditDepartmentSmallGroupsCommandState {
	self: SmallGroupServiceComponent =>

	override def applyInternal() = {
		groupNames.asScala.zipWithIndex.foreach { case (name, i) =>
			if (set.groups.size() > i) {
				// Edit an existing group
				set.groups.get(i).name = name
			} else {
				// Add a new group
				val group = new DepartmentSmallGroup(set)
				group.name = name

				set.groups.add(group)

				// We also need to create a linked group elsewhere for any sets linked to this set
				set.linkedSets.asScala.foreach { smallGroupSet =>
					val smallGroup = new SmallGroup
					smallGroup.name = name
					smallGroup.linkedDepartmentSmallGroup = group
					smallGroupSet.groups.add(smallGroup)
				}
			}
		}

		if (groupNames.size() < set.groups.size()) {
			for (i <- set.groups.size() until groupNames.size() by -1) {
				val group = set.groups.get(i - 1)

				// Remove any linked groups
				group.linkedGroups.asScala.map { smallGroup =>
					smallGroup.preDelete()
					smallGroup
				}.foreach { smallGroup =>
					smallGroup.groupSet.groups.remove(smallGroup)
				}

				set.groups.remove(i - 1)


			}
		}

		smallGroupService.saveOrUpdate(set)
		set.linkedSets.asScala.foreach(smallGroupService.saveOrUpdate)

		set.groups.asScala
	}
}

trait PopulateEditDepartmentSmallGroupsCommand extends PopulateOnForm {
	self: EditDepartmentSmallGroupsCommandState =>

	override def populate() {
		groupNames.clear()
		groupNames.addAll(set.groups.asScala.map { _.name }.sorted.asJava)
	}
}

trait EditDepartmentSmallGroupsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditDepartmentSmallGroupsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, department)
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
	}
}

trait EditDepartmentSmallGroupsDescription extends Describable[Seq[DepartmentSmallGroup]] {
	self: EditDepartmentSmallGroupsCommandState =>

	override def describe(d: Description) {
		d.department(set.department).properties("smallGroupSet" -> set.id)
	}

}

trait EditDepartmentSmallGroupsValidation extends SelfValidating {
	self: EditDepartmentSmallGroupsCommandState with SmallGroupServiceComponent =>

	override def validate(errors: Errors) {
		groupNames.asScala.zipWithIndex.foreach { case (name, index) =>
			if (!name.hasText) errors.rejectValue(s"groupNames[$index]", "smallGroup.name.NotEmpty")
			else if (name.orEmpty.length > 200) errors.rejectValue(s"groupNames[$index]", "smallGroup.name.Length", Array[Object](200: JInteger), "")
		}

		val postedGroupNameSize = groupNames.size
		set.groups.asScala.filterNot(g => groupNames.contains(g.name)).zipWithIndex.foreach { case (group, index) =>
			if (!group.students.isEmpty) {
				groupNames.add(group.name) // Add the group name back in so the error message makes sense
				errors.rejectValue(s"groupNames[${postedGroupNameSize + index}]", "smallGroup.delete.notEmpty")
			} else {
				val hasAttendance =
					group.linkedGroups.asScala.flatMap(_.events).exists { event =>
						smallGroupService.getAllSmallGroupEventOccurrencesForEvent(event)
							.exists {
								_.attendance.asScala.exists { attendance =>
									attendance.state != AttendanceState.NotRecorded
								}
							}
					}

				if (hasAttendance) {
					groupNames.add(group.name) // Add the group name back in so the error message makes sense
					errors.rejectValue(s"groupNames[${postedGroupNameSize + index}]", "smallGroupEvent.delete.hasAttendance")
				}
			}
		}
	}
}

trait EditDepartmentSmallGroupsCommandRemoveTrailingEmptyGroups extends BindListener {
	self: EditDepartmentSmallGroupsCommandState =>

	override def onBind(result: BindingResult) {
		// If the last element of events is both a Creation and is empty, disregard it
		while (!groupNames.isEmpty && !groupNames.asScala.last.hasText) {
			groupNames.remove(groupNames.asScala.last)
		}
	}
}