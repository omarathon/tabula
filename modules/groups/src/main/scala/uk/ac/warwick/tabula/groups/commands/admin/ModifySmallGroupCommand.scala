package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.PromisingCommand
import uk.ac.warwick.tabula.helpers.LazyLists
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.UniversityId
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.StringUtils._

/**
 * Common superclass for creation and modification. Note that any defaults on the vars here are defaults
 * for creation; the Edit command should call .copyFrom(SmallGroup) to copy any existing properties.
 */
abstract class ModifySmallGroupCommand(module: Module, properties: SmallGroupSetProperties)
	extends PromisingCommand[SmallGroup] with SelfValidating with BindListener {

	var userLookup = Wire[UserLookupService]

	var name: String = _

	var maxGroupSize: Int = if (properties.defaultMaxGroupSizeEnabled) properties.defaultMaxGroupSize else SmallGroup.DefaultGroupSize

	// Used by parent command
	var delete: Boolean = false

	// A collection of sub-commands for modifying the events
	var events: JList[ModifySmallGroupEventCommand] = LazyLists.create { () =>
		new CreateSmallGroupEventCommand(this, module)
	}

	def validate(errors: Errors) {
		// Skip validation when this group is being deleted
		if (!delete) {
			if (!name.hasText) errors.rejectValue("name", "smallGroup.name.NotEmpty")
			else if (name.orEmpty.length > 200) errors.rejectValue("name", "smallGroup.name.Length", Array[Object](200: JInteger), "")

			events.asScala.zipWithIndex foreach { case (cmd, index) =>
				errors.pushNestedPath("events[" + index + "]")
				cmd.validate(errors)
				errors.popNestedPath()
			}
		}
	}

	def copyFrom(group: SmallGroup) {
		name = group.name

		group.maxGroupSize.foreach(size => maxGroupSize = size)

		events.clear()
		events.addAll(group.events.asScala.map(new EditSmallGroupEventCommand(_)).asJava)
	}

	def copyTo(group: SmallGroup) {
		group.name = name

		group.maxGroupSize = maxGroupSize

		// Clear the groups on the set and add the result of each command; this may result in a new group or an existing one.
		group.events.clear()
		for (event <- events.asScala.filter(!_.delete).map(_.apply())) {
			// make sure we set the back-reference from event->group here, else
      // we won't be able to navigate back up the tree unless we reload the data from hiberate
      event.group = group
      group.events.add(event)
    }
	}

	override def onBind(result: BindingResult) {
		// If the last element of events is both a Creation and is empty, disregard it
		if (!events.isEmpty()) {
			val last = events.asScala.last

			last match {
				case cmd: CreateSmallGroupEventCommand if cmd.isEmpty =>
					events.remove(last)
				case _ => // do nothing
			}
		}

		events.asScala.foreach(_.onBind(result))
	}
}