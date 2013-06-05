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
import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.system.BindListener
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup
import uk.ac.warwick.tabula.helpers.StringUtils._
import org.hibernate.validator.Valid
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod

/**
 * Common superclass for creation and modification. Note that any defaults on the vars here are defaults
 * for creation; the Edit command should call .copyFrom(SmallGroupSet) to copy any existing properties.
 */
abstract class ModifySmallGroupSetCommand(val module: Module) 
	extends PromisingCommand[SmallGroupSet]  
		with SelfValidating 
		with BindListener {
	
	var userLookup = Wire[UserLookupService]
	var membershipService = Wire[AssignmentMembershipService]
	
	var name: String = _

	var academicYear: AcademicYear = AcademicYear.guessByDate(DateTime.now)
	
	var format: SmallGroupFormat = _
	
	var allocationMethod: SmallGroupAllocationMethod = SmallGroupAllocationMethod.Manual
	
	// start complicated membership stuff
	
	var assessmentGroups: JList[UpstreamAssessmentGroup] = JArrayList()
	
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
		if (!name.hasText) errors.rejectValue("name", "smallGroupSet.name.NotEmpty")
		else if (name.orEmpty.length > 200) errors.rejectValue("name", "smallGroupSet.name.Length", Array[Object](200: JInteger), "")
		
		if (format == null) errors.rejectValue("format", "smallGroupSet.format.NotEmpty")
		if (allocationMethod == null) errors.rejectValue("allocationMethod", "smallGroupSet.allocationMethod.NotEmpty")
		
		groups.asScala.zipWithIndex foreach { case (cmd, index) =>
			errors.pushNestedPath("groups[" + index + "]")
			cmd.validate(errors)
			errors.popNestedPath()
		}
	}
	
	def copyFrom(set: SmallGroupSet) {
		name = set.name
		academicYear = set.academicYear
		format = set.format
		allocationMethod = set.allocationMethod
		
		// TODO AssessmentGroupItems
		
		groups.clear()
		groups.addAll(set.groups.asScala.map(new EditSmallGroupCommand(_)).asJava)
		
		if (set.members != null) members.copyFrom(set.members)
	}
	
	def copyTo(set: SmallGroupSet) {
		set.name = name
		set.academicYear = academicYear
		set.format = format
		set.allocationMethod = allocationMethod
		
		// TODO AssessmentGroupItems
		
		// Clear the groups on the set and add the result of each command; this may result in a new group or an existing one.
		set.groups.clear()
		set.groups.addAll(groups.asScala.filter(!_.delete).map(_.apply()).asJava)
		
		if (set.members == null) set.members = new UserGroup
		set.members.copyFrom(members)
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

		val membersUserIds = 
			membershipService.determineMembershipUsers(assessmentGroups.asScala, Some(members))
			.map(_.getUserId)

		// add includeUsers to members.includeUsers
		((includeUsers.asScala.map { _.trim }.filterNot { _.isEmpty }).distinct) foreach { userId =>
			if (members.excludeUsers contains userId) {
				members.unexcludeUser(userId)
			} else if (!(membersUserIds contains userId)) {
				// TAB-399 need to check if the user is already in the group before adding
				members.addUser(userId)
			}
		}

		// for excludeUsers, either remove from previously-added users or add to excluded users.
		((excludeUsers.asScala.map { _.trim }.filterNot { _.isEmpty }).distinct) foreach { userId =>
			if (members.includeUsers contains userId) members.removeUser(userId)
			else members.excludeUser(userId)
		}

		// empty these out to make it clear that we've "moved" the data into members
		massAddUsers = ""
			
		// If the last element of groups is both a Creation and is empty, disregard it
		def isEmpty(cmd: ModifySmallGroupCommand) = cmd match {
			case cmd: CreateSmallGroupCommand if !cmd.name.hasText && cmd.events.isEmpty => true
			case _ => false
		}
		
		while (!groups.isEmpty() && isEmpty(groups.asScala.last))
			groups.remove(groups.asScala.last)
		
		groups.asScala.foreach(_.onBind(result))
	}

}