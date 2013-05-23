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
import uk.ac.warwick.tabula.helpers.LazyLists
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.helpers.Promise
import uk.ac.warwick.tabula.helpers.Promises._
import uk.ac.warwick.tabula.commands.PromisingCommand
import scala.collection.JavaConverters._

/**
 * Common superclass for creation and modification. Note that any defaults on the vars here are defaults
 * for creation; the Edit command should call .copyFrom(SmallGroupSet) to copy any existing properties.
 */
abstract class ModifySmallGroupSetCommand(val module: Module) extends PromisingCommand[SmallGroupSet] with Promise[SmallGroupSet] with SelfValidating {
	
	@Length(max = 200)
	@NotEmpty(message = "{NotEmpty.smallGroupSetName}")
	var name: String = _

	var academicYear: AcademicYear = AcademicYear.guessByDate(DateTime.now)
	
	@NotEmpty
	var format: SmallGroupFormat = _
	
	// start complicated membership stuff
	
	var assessmentGroups: JList[AssessmentGroupItem] = JArrayList()
	
	/**
	 * If copying from existing SmallGroupSet, this must be a DEEP COPY
	 * with changes copied back to the original UserGroup, don't pass
	 * the same UserGroup around because it'll just cause Hibernate
	 * problems. This copy should be transient.
	 *
	 * Changes to members are done via includeUsers and excludeUsers, since
	 * it is difficult to bind additions and removals directly to a collection
	 * with Spring binding.
	 */
	var members: UserGroup = new UserGroup

	// items added here are added to members.includeUsers.
	var includeUsers: JList[String] = JArrayList()

	// items added here are either removed from members.includeUsers or added to members.excludeUsers.
	var excludeUsers: JList[String] = JArrayList()

	// bind property for the big free-for-all textarea of usercodes/uniIDs to add.
	// These are first resolved to userIds and then added to includeUsers
	var massAddUsers: String = _

	// parse massAddUsers into a collection of individual tokens
	def massAddUsersEntries: Seq[String] =
		if (massAddUsers == null) Nil
		else massAddUsers split ("\\s+") map (_.trim) filterNot (_.isEmpty)

	///// end of complicated membership stuff
		
	// A collection of sub-commands for modifying the child groups
	var groups: JList[ModifySmallGroupCommand] = LazyLists.withFactory { () => 
		new CreateSmallGroupCommand(this, module)
	}
	
	def validate(errors: Errors) {
		// TODO
	}
	
	def copyFrom(set: SmallGroupSet) {
		name = set.name
		academicYear = set.academicYear
		format = set.format
		
		// TODO AssessmentGroupItems
		
		groups.clear()
		groups.addAll(set.groups.asScala.map(new EditSmallGroupCommand(_)).asJava)
		
		if (set.members != null) members.copyFrom(set.members)
	}
	
	def copyTo(set: SmallGroupSet) {
		set.name = name
		set.academicYear = academicYear
		set.format = format
		
		// TODO AssessmentGroupItems
		
		// Clear the groups on the set and add the result of each command; this may result in a new group or an existing one.
		// CONSIDER How will deletions be handled?
		set.groups.clear()
		set.groups.addAll(groups.asScala.map(_.apply()).asJava)
		
		if (set.members == null) set.members = new UserGroup
		set.members.copyFrom(members)
	}

}

class AssessmentGroupItem() {
	
}