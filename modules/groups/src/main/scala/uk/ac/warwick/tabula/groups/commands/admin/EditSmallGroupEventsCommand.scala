package uk.ac.warwick.tabula.groups.commands.admin

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
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
	type SmallGroupEventCommand = ModifySmallGroupEventCommand.Command

	def apply(module: Module, set: SmallGroupSet) =
		new EditSmallGroupEventsCommandInternal(module, set)
			with ComposableCommand[SmallGroupSet]
			with EditSmallGroupEventsPermissions
			with EditSmallGroupEventsValidation
			with EditSmallGroupEventsDescription
			with EditSmallGroupEventsBinding
			with AutowiringSmallGroupServiceComponent
			with PopulateEditSmallGroupEventsSubCommands
}

class GroupProperties(val module: Module, val set: SmallGroupSet, val group: SmallGroup) {
	var events: JList[SmallGroupEventCommand] = LazyLists.create { () =>
		ModifySmallGroupEventCommand.create(module, set, group)
	}

	group.events.asScala.foreach { event =>
		events.add(ModifySmallGroupEventCommand.edit(module, set, group, event))
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
	self: SmallGroupServiceComponent =>

	override def applyInternal() = transactional() {
		groups.asScala.foreach { case (group, props) =>
			val events = props.events

			// Clear the groups on the set and add the result of each command; this may result in a new group or an existing one.
			// TAB-2304 Don't do a .clear() and .addAll() because that confuses Hibernate
			val newEvents = events.asScala.filter(!_.delete).map(_.apply())
			group.events.asScala.filterNot(newEvents.contains).foreach(group.events.remove)
			newEvents.filterNot(group.events.contains).foreach { event =>
				// make sure we set the back-reference from event->group here, else
				// we won't be able to navigate back up the tree unless we reload the data from hiberate
				event.group = group
				group.events.add(event)
			}
		}

		smallGroupService.saveOrUpdate(set)
		set
	}
}

trait EditSmallGroupEventsValidation extends SelfValidating {
	self: EditSmallGroupEventsCommandState =>

	override def validate(errors: Errors) {
		groups.asScala.foreach { case (group, props) =>
			val events = props.events

			errors.pushNestedPath(s"groups[${group.id}]")
			events.asScala.zipWithIndex.foreach { case (command, index) =>
				errors.pushNestedPath(s"events[${index}]")
				command.validate(errors)
				errors.popNestedPath()
			}
			errors.popNestedPath()
		}
	}
}

trait EditSmallGroupEventsBinding extends BindListener {
	self: EditSmallGroupEventsCommandState =>

	override def onBind(result: BindingResult) {
		groups.asScala.values.map { _.events }.foreach { events =>
			// If the last element of events is both a Creation and is empty, disregard it
			if (!events.isEmpty()) {
				val last = events.asScala.last

				last match {
					case cmd: CreateSmallGroupEventCommandState if cmd.isEmpty =>
						events.remove(last)
					case _ => // do nothing
				}
			}

			events.asScala.foreach(_.onBind(result))
		}
	}
}

trait EditSmallGroupEventsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditSmallGroupEventsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, module)
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
	}
}

trait EditSmallGroupEventsDescription extends Describable[SmallGroupSet] {
	self: EditSmallGroupEventsCommandState =>

	override def describe(d: Description) {
		d.smallGroupSet(set)
	}

}