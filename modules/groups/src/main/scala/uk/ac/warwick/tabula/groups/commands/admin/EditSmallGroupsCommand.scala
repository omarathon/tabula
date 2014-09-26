package uk.ac.warwick.tabula.groups.commands.admin

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.SmallGroupAttendanceState
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._

object EditSmallGroupsCommand {
	def apply(module: Module, set: SmallGroupSet) =
		new EditSmallGroupsCommandInternal(module, set)
			with ComposableCommand[Seq[SmallGroup]]
			with EditSmallGroupsPermissions
			with EditSmallGroupsValidation
			with EditSmallGroupsDescription
			with PopulateEditSmallGroupsCommand
			with EditSmallGroupsCommandRemoveTrailingEmptyGroups
			with AutowiringSmallGroupServiceComponent
}

trait EditSmallGroupsCommandState {
	def module: Module
	def set: SmallGroupSet

	var groupNames: JList[String] = LazyLists.create()
	var maxGroupSizes: JList[JInteger] = LazyLists.create(() => 0)
	var defaultMaxGroupSizeEnabled: Boolean = false
	var defaultMaxGroupSize: Int = SmallGroup.DefaultGroupSize
}

class EditSmallGroupsCommandInternal(val module: Module, val set: SmallGroupSet) extends CommandInternal[Seq[SmallGroup]] with EditSmallGroupsCommandState {
	self: SmallGroupServiceComponent =>

	override def applyInternal() = {
		set.defaultMaxGroupSizeEnabled = defaultMaxGroupSizeEnabled
		if (defaultMaxGroupSizeEnabled) set.defaultMaxGroupSize = defaultMaxGroupSize

		groupNames.asScala.zipWithIndex.foreach { case (name, i) =>
			val group =
				if (set.groups.size() > i) {
					// Edit an existing group
					set.groups.get(i)
				} else {
					// Add a new group
					val group = new SmallGroup(set)
					set.groups.add(group)

					group
				}

			group.name = name

			if (defaultMaxGroupSizeEnabled) {
				group.maxGroupSize = maxGroupSizes.get(i)
			} else {
				group.removeMaxGroupSize()
			}
		}

		if (groupNames.size() < set.groups.size()) {
			for (i <- set.groups.size() until groupNames.size() by -1) {
				val group = set.groups.get(i - 1)
				set.groups.remove(group)
			}
		}

		smallGroupService.saveOrUpdate(set)
		set.groups.asScala
	}
}

trait PopulateEditSmallGroupsCommand extends PopulateOnForm {
	self: EditSmallGroupsCommandState =>

	override def populate() {
		groupNames.clear()
		maxGroupSizes.clear()

		groupNames.addAll(set.groups.asScala.map { _.name }.asJava)
		maxGroupSizes.addAll(set.groups.asScala.map { group =>
			val groupSize = group.maxGroupSize.getOrElse {
				if (set.defaultMaxGroupSizeEnabled) set.defaultMaxGroupSize else SmallGroup.DefaultGroupSize
			}

			groupSize: JInteger
		}.asJava)
		defaultMaxGroupSizeEnabled = set.defaultMaxGroupSizeEnabled
		defaultMaxGroupSize = set.defaultMaxGroupSize
	}
}

trait EditSmallGroupsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditSmallGroupsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, module)
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
	}
}

trait EditSmallGroupsDescription extends Describable[Seq[SmallGroup]] {
	self: EditSmallGroupsCommandState =>

	override def describe(d: Description) {
		d.smallGroupSet(set)
	}

}

trait EditSmallGroupsValidation extends SelfValidating {
	self: EditSmallGroupsCommandState with SmallGroupServiceComponent =>

	override def validate(errors: Errors) {
		if (set.allocationMethod == SmallGroupAllocationMethod.Linked) {
			errors.reject("smallGroupSet.linked")
		}

		groupNames.asScala.zipWithIndex.foreach { case (name, index) =>
			if (!name.hasText) errors.rejectValue(s"groupNames[$index]", "smallGroup.name.NotEmpty")
			else if (name.orEmpty.length > 200) errors.rejectValue(s"groupNames[$index]", "smallGroup.name.Length", Array[Object](200: JInteger), "")
		}

		if (groupNames.size() < set.groups.size()) {
			for (i <- set.groups.size() until groupNames.size() by -1) {
				val group = set.groups.get(i - 1)

				if (!group.students.isEmpty) {
					errors.rejectValue(s"groupNames[${i - 1}]", "smallGroup.delete.notEmpty")
				} else {
					val hasAttendance =
						group.events.exists { event =>
							smallGroupService.getAllSmallGroupEventOccurrencesForEvent(event)
								.exists { _.attendance.asScala.exists { attendance =>
								attendance.state != AttendanceState.NotRecorded
							}}
						}

					if (hasAttendance) {
						errors.rejectValue(s"groupNames[${i - 1}]", "smallGroupEvent.delete.hasAttendance")
					}
				}
			}
		}
	}
}

trait EditSmallGroupsCommandRemoveTrailingEmptyGroups extends BindListener {
	self: EditSmallGroupsCommandState =>

	override def onBind(result: BindingResult) {
		// If the last element of events is both a Creation and is empty, disregard it
		while (!groupNames.isEmpty && !groupNames.asScala.last.hasText) {
			groupNames.remove(groupNames.asScala.last)
		}
	}
}