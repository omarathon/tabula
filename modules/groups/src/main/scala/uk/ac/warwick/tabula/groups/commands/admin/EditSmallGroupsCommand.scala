package uk.ac.warwick.tabula.groups.commands.admin

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
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
}

class EditSmallGroupsCommandInternal(val module: Module, val set: SmallGroupSet) extends CommandInternal[Seq[SmallGroup]] with EditSmallGroupsCommandState {
	self: SmallGroupServiceComponent =>

	override def applyInternal() = {
		groupNames.asScala.zipWithIndex.foreach { case (name, i) =>
			if (set.groups.size() > i) {
				// Edit an existing group
				set.groups.get(i).name = name
			} else {
				// Add a new group
				val group = new SmallGroup(set)
				group.name = name

				set.groups.add(group)
			}
		}

		if (groupNames.size() < set.groups.size()) {
			for (i <- set.groups.size() until groupNames.size() by -1) {
				val group = set.groups.get(i - 1)
				set.groups.remove(i - 1)
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
		groupNames.addAll(set.groups.asScala.map { _.name }.asJava)
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
	self: EditSmallGroupsCommandState =>

	override def validate(errors: Errors) {
		groupNames.asScala.zipWithIndex.foreach { case (name, index) =>
			if (!name.hasText) errors.rejectValue(s"groupNames[${index}]", "smallGroup.name.NotEmpty")
			else if (name.orEmpty.length > 200) errors.rejectValue(s"groupNames[${index}]", "smallGroup.name.Length", Array[Object](200: JInteger), "")
		}

		if (groupNames.size() < set.groups.size()) {
			for (i <- set.groups.size() until groupNames.size() by -1) {
				val group = set.groups.get(i - 1)

				if (!group.students.isEmpty) {
					errors.rejectValue(s"groupNames[${i - 1}]", "smallGroup.delete.notEmpty")
				}
			}
		}
	}
}

trait EditSmallGroupsCommandRemoveTrailingEmptyGroups extends BindListener {
	self: EditSmallGroupsCommandState =>

	override def onBind(result: BindingResult) {
		// If the last element of events is both a Creation and is empty, disregard it
		while (!groupNames.isEmpty() && !groupNames.asScala.last.hasText) {
			groupNames.remove(groupNames.asScala.last)
		}
	}
}