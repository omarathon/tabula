package uk.ac.warwick.tabula.groups.commands.admin

import org.hibernate.validator.constraints._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.data.model.{AssessmentGroup, Module, UserGroup, UpstreamAssessmentGroup}
import uk.ac.warwick.tabula.commands.{UpdatesStudentMembership, SelfValidating, PromisingCommand}
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
import scala.collection.JavaConversions._
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
		with SmallGroupSetProperties
		with UpdatesStudentMembership
		with SelfValidating 
		with BindListener {

	// get these from UpdatesStudentMembership
	//var userLookup = Wire[UserLookupService]
	//var membershipService = Wire[AssignmentMembershipService]
	//var academicYear: AcademicYear = AcademicYear.guessByDate(DateTime.now)

	val setOption : Option[SmallGroupSet]

	var name: String = _
	
	// start complicated membership stuff

	lazy val exisitingGroups: Option[Seq[UpstreamAssessmentGroup]] =  setOption.map(_.upstreamAssessmentGroups)
	lazy val existingMembers: Option[UserGroup] = setOption.map(_.members)

	/**
	 * Convert Spring-bound upstream group references to an AssessmentGroup buffer
	 */
	def updateAssessmentGroups() {
		assessmentGroups = upstreamGroups.asScala.flatMap ( ug => {
			val template = new AssessmentGroup
			template.upstreamAssignment = ug.upstreamAssignment
			template.occurrence = ug.occurrence
			template.smallGroupSet = setOption.getOrElse(null)
			membershipService.getAssessmentGroup(template) orElse Some(template)
		}).distinct.asJava
	}

	// end of complicated membership stuff
		
	// A collection of sub-commands for modifying the child groups
	var groups: JList[ModifySmallGroupCommand] = LazyLists.withFactory { () => 
		new CreateSmallGroupCommand(this, module, this)
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

		for (group <- set.assessmentGroups.asScala if group.smallGroupSet == null) {
			group.smallGroupSet = set // only required for a new assignment
		}
		assessmentGroups = set.assessmentGroups
		allowSelfGroupSwitching = set.allowSelfGroupSwitching
		studentsCanSeeTutorName = set.studentsCanSeeTutorName
	  studentsCanSeeOtherMembers = set.studentsCanSeeOtherMembers
		defaultMaxGroupSizeEnabled = set.defaultMaxGroupSizeEnabled
		defaultMaxGroupSize = set.defaultMaxGroupSize

		
		groups.clear()
		groups.addAll(set.groups.asScala.map(x => {new EditSmallGroupCommand(x, this)}).asJava)
		
		if (set.members != null) members.copyFrom(set.members)
	}
	
	def copyTo(set: SmallGroupSet) {
		set.name = name
		set.academicYear = academicYear
		set.format = format
		set.allocationMethod = allocationMethod

		set.assessmentGroups = assessmentGroups
		
		set.allowSelfGroupSwitching = allowSelfGroupSwitching
		set.studentsCanSeeOtherMembers = studentsCanSeeOtherMembers
		set.studentsCanSeeTutorName = studentsCanSeeTutorName
		set.defaultMaxGroupSizeEnabled = defaultMaxGroupSizeEnabled
		set.defaultMaxGroupSize = defaultMaxGroupSize

		// TODO AssessmentGroupItems
		
		// Clear the groups on the set and add the result of each command; this may result in a new group or an existing one.
		set.groups.clear()
		set.groups.addAll(groups.asScala.filter(!_.delete).map(_.apply()).asJava)
		
		if (set.members == null) set.members = new UserGroup
		set.members.copyFrom(members)
	}
	
	override def onBind(result: BindingResult) {

		afterBind()
			
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


trait SmallGroupSetProperties {
	var name: String = _

	var academicYear: AcademicYear = AcademicYear.guessByDate(DateTime.now)

	var format: SmallGroupFormat = _

	var allocationMethod: SmallGroupAllocationMethod = SmallGroupAllocationMethod.Manual

	var allowSelfGroupSwitching: Boolean = true
	var studentsCanSeeTutorName:Boolean = false
	var studentsCanSeeOtherMembers:Boolean = false
	var defaultMaxGroupSizeEnabled:Boolean = false
	var defaultMaxGroupSize:Int = SmallGroup.DefaultGroupSize
}