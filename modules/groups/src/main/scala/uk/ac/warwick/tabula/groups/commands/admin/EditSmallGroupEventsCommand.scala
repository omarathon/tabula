package uk.ac.warwick.tabula.groups.commands.admin

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import scala.collection.JavaConverters._
import EditSmallGroupEventsCommand._

object EditSmallGroupEventsCommand {
	def apply(module: Module, set: SmallGroupSet) =
		new EditSmallGroupEventsCommandInternal(module, set)
			with ComposableCommand[SmallGroupSet]
			with DeletesSmallGroupEventsWithCommand
			with EditSmallGroupEventsPermissions
			with EditSmallGroupEventsValidation
			with Unaudited
			with AutowiringSmallGroupServiceComponent
			with PopulateEditSmallGroupEventsSubCommands
}

class EventProperties(val event: SmallGroupEvent) {
	var delete: Boolean = false
}

class GroupProperties(val module: Module, val set: SmallGroupSet, val group: SmallGroup) {
	var events: JList[EventProperties] = JArrayList()

	group.events.asScala.foreach { event =>
		events.add(new EventProperties(event))
	}
}

trait EditSmallGroupEventsCommandState {
	def module: Module
	def set: SmallGroupSet

	var groups: JMap[SmallGroup, GroupProperties] = JHashMap()
}

trait PopulateEditSmallGroupEventsSubCommands {
	self: EditSmallGroupEventsCommandState =>

	groups.clear()
	set.groups.asScala.foreach { group =>
		groups.put(group, new GroupProperties(module, set, group))
	}
}

class EditSmallGroupEventsCommandInternal(val module: Module, val set: SmallGroupSet) extends CommandInternal[SmallGroupSet] with EditSmallGroupEventsCommandState {
	self: SmallGroupServiceComponent with DeletesSmallGroupEvents =>

	override def applyInternal() = transactional() {
		groups.asScala.foreach { case (group, props) =>
			props.events.asScala.filter(_.delete).map(_.event).foreach { event =>
				deleteEvent(group, event)
			}
		}

		smallGroupService.saveOrUpdate(set)
		set
	}
}

trait DeletesSmallGroupEvents {
	def deleteEvent(group: SmallGroup, event: SmallGroupEvent)
}

trait DeletesSmallGroupEventsWithCommand extends DeletesSmallGroupEvents {
	def deleteEvent(group: SmallGroup, event: SmallGroupEvent) {
		DeleteSmallGroupEventCommand(group, event).apply()
	}
}

trait EditSmallGroupEventsValidation extends SelfValidating {
	self: EditSmallGroupEventsCommandState =>

	override def validate(errors: Errors) {
		// TODO Do we need to validate whether we're allowed to delete, or is it always ok?
	}
}

trait EditSmallGroupEventsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditSmallGroupEventsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, module)
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
	}
}