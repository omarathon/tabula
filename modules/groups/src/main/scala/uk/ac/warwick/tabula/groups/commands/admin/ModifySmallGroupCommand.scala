package uk.ac.warwick.tabula.groups.commands.admin

import org.hibernate.validator.constraints._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.helpers.Promise
import uk.ac.warwick.tabula.commands.PromisingCommand
import uk.ac.warwick.tabula.helpers.LazyLists
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.UniversityId
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.services.{SmallGroupService, UserLookupService}
import uk.ac.warwick.spring.Wire
import org.hibernate.validator.Valid
import uk.ac.warwick.tabula.helpers.StringUtils._

/**
 * Common superclass for creation and modification. Note that any defaults on the vars here are defaults
 * for creation; the Edit command should call .copyFrom(SmallGroup) to copy any existing properties.
 */
abstract class ModifySmallGroupCommand(module: Module, properties: SmallGroupSetProperties) extends PromisingCommand[SmallGroup] with SelfValidating with BindListener {

	var userLookup = Wire[UserLookupService]
	
	var name: String = _

	var maxGroupSize: Int = if (properties.defaultMaxGroupSizeEnabled) properties.defaultMaxGroupSize else SmallGroup.DefaultGroupSize

	// Used by parent command
	var delete: Boolean = false
	
	// start complicated membership stuff
	
	/**
	 * If copying from existing SmallGroup, this must be a DEEP COPY
	 * with changes copied back to the original UserGroup, don't pass
	 * the same UserGroup around because it'll just cause Hibernate
	 * problems. This copy should be transient.
	 *
	 * Changes to members are done via includeUsers and excludeUsers, since
	 * it is difficult to bind additions and removals directly to a collection
	 * with Spring binding.
	 */
	var students: UserGroup = new UserGroup

	// items added here are added to members.includeUsers.
	var includeUsers: JList[String] = JArrayList()

	// bind property for the big free-for-all textarea of usercodes/uniIDs to add.
	// These are first resolved to userIds and then added to includeUsers
	var massAddUsers: String = _

	// parse massAddUsers into a collection of individual tokens
	def massAddUsersEntries: Seq[String] =
		if (massAddUsers == null) Nil
		else massAddUsers split ("\\s+") map (_.trim) filterNot (_.isEmpty)

	///// end of complicated membership stuff
		
	// A collection of sub-commands for modifying the events
	var events: JList[ModifySmallGroupEventCommand] = LazyLists.withFactory { () => 
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

		if (group.students != null) students.copyFrom(group.students)
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

		
		if (group.students == null) group.students = new UserGroup
		group.students.copyFrom(students)
	}
	
	override def onBind(result: BindingResult) {
		def addUserId(item: String) {
			val user = userLookup.getUserByUserId(item)
			if (user.isFoundUser && null != user.getWarwickId) {
				includeUsers.add(user.getUserId)
			}
		}

		// parse items from textarea into includeUsers collection
		for (item <- massAddUsersEntries) {
			if (UniversityId.isValid(item)) {
				val user = userLookup.getUserByWarwickUniId(item)
				if (user.isFoundUser) {
					includeUsers.add(user.getUserId)
				} else {
					addUserId(item)
				}
			} else {
				addUserId(item)
			}
		}

		// add includeUsers to members.includeUsers
		((includeUsers.asScala.map { _.trim }.filterNot { _.isEmpty }).distinct) foreach { userId =>
			students.addUser(userId)
		}

		// empty these out to make it clear that we've "moved" the data into members
		massAddUsers = ""
			
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