package uk.ac.warwick.tabula.data.model.groups

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

import javax.persistence._
import javax.persistence.CascadeType._
import javax.validation.constraints.NotNull

import org.hibernate.annotations.{AccessType, Filter, FilterDef, Type}
import org.joda.time.DateTime

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.data.PostLoadBehaviour

object SmallGroupSet {
	final val NotDeletedFilter = "notDeleted"
	object Settings {
		val StudentsCanSeeTutorNames = "StudentsCanSeeTutorNames"
		val StudentsCanSeeOtherMembers = "StudentsCanSeeOtherMembers"
		val DefaultMaxGroupSizeEnabled = "DefaultMaxGroupSizeEnabled"
		val DefaultMaxGroupSize = "DefaultMaxGroupSize"
	}
}

/**
 * Represents a set of small groups, within an instance of a module.
 */
@FilterDef(name = SmallGroupSet.NotDeletedFilter, defaultCondition = "deleted = 0")
@Filter(name = SmallGroupSet.NotDeletedFilter)
@Entity
@AccessType("field")
class SmallGroupSet extends GeneratedId with CanBeDeleted with ToString with PermissionsTarget with HasSettings with Serializable with PostLoadBehaviour  {
	import SmallGroupSet.Settings
	import SmallGroup._

	@transient var permissionsService = Wire[PermissionsService]
	@transient var membershipService = Wire[AssignmentMembershipService]

	def this(_module: Module) {
		this()
		this.module = _module
	}

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Column(nullable = false)
	var academicYear: AcademicYear = AcademicYear.guessByDate(new DateTime())

	@NotNull
	var name: String = _

	var archived: JBoolean = false

  @Column(name="released_to_students")
	var releasedToStudents: JBoolean = false
  @Column(name="released_to_tutors")
  var releasedToTutors: JBoolean = false

  def fullyReleased= releasedToStudents && releasedToTutors

	@Column(name="group_format")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.groups.SmallGroupFormatUserType")
	@NotNull
	var format: SmallGroupFormat = _
	
	@Column(name="allocation_method")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethodUserType")
	var allocationMethod: SmallGroupAllocationMethod = _

	@Column(name="self_group_switching")
	var allowSelfGroupSwitching: Boolean = true

	// TODO consider changing this to be a string, and setting it to the name of the SmallGroupSetSelfSignUpState
	// to allow for more states than just "open" and "closed"
	@Column(name="open_for_signups")
	var openForSignups: Boolean = false

  def openState:SmallGroupSetSelfSignUpState = if (openForSignups) SmallGroupSetSelfSignUpState.Open else SmallGroupSetSelfSignUpState.Closed 
  
 def openState_= (value:SmallGroupSetSelfSignUpState):Unit =  value match {
	  case SmallGroupSetSelfSignUpState.Open => openForSignups = true
	  case SmallGroupSetSelfSignUpState.Closed => openForSignups = false
  }
	
	@ManyToOne
	@JoinColumn(name = "module_id")
	var module: Module = _
	
	@OneToMany(fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval=true)
	@JoinColumn(name = "set_id")
	var groups: JList[SmallGroup] = JArrayList()

	// only students manually added or excluded. use allStudents to get all students in the group set
	@OneToOne(cascade = Array(ALL))
	@JoinColumn(name = "membersgroup_id")
	var _membersGroup = UserGroup.ofUniversityIds
	def members: UnspecifiedTypeUserGroup= _membersGroup

	// Cannot link directly to upstream assessment groups data model in sits is silly ...
	@OneToMany(mappedBy = "smallGroupSet", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	var assessmentGroups: JList[AssessmentGroup] = JArrayList()

	// converts the assessmentGroups to upstream assessment groups
	def upstreamAssessmentGroups: Seq[UpstreamAssessmentGroup] = {
		if(academicYear == null){
			Seq()
		}
		else {
			val validGroups = assessmentGroups.asScala.filterNot(group=> group.upstreamAssignment == null || group.occurrence == null)
			validGroups.flatMap { group =>
				val template = new UpstreamAssessmentGroup
				template.academicYear = academicYear
				template.assessmentGroup = group.upstreamAssignment.assessmentGroup
				template.moduleCode = group.upstreamAssignment.moduleCode
				template.occurrence = group.occurrence
				membershipService.getUpstreamAssessmentGroup(template)
			}
		}
	}

	def allStudents = membershipService.determineMembershipUsers(upstreamAssessmentGroups, Some(members))
	def allStudentsCount = membershipService.countMembershipUsers(upstreamAssessmentGroups, Some(members))
	
	def unallocatedStudents = {
		val allocatedStudents = groups.asScala flatMap { _.students.users }

		allStudents diff allocatedStudents
	}
	
	def unallocatedStudentsCount = {
		val allocatedStudentsCount = groups.asScala.foldLeft(0) { (acc, grp) => acc + grp.students.size }
		
		allStudentsCount - allocatedStudentsCount
	}
	
	def hasAllocated = groups.asScala exists { !_.students.isEmpty }
	
	def permissionsParents = Option(module).toStream

	def studentsCanSeeTutorName = getBooleanSetting(Settings.StudentsCanSeeTutorNames).getOrElse(false)
	def studentsCanSeeTutorName_=(canSee:Boolean) = settings += (Settings.StudentsCanSeeTutorNames -> canSee)

	def studentsCanSeeOtherMembers = getBooleanSetting(Settings.StudentsCanSeeOtherMembers).getOrElse(false)
	def studentsCanSeeOtherMembers_=(canSee:Boolean) = settings += (Settings.StudentsCanSeeOtherMembers -> canSee)

	def defaultMaxGroupSizeEnabled = getBooleanSetting(Settings.DefaultMaxGroupSizeEnabled).getOrElse(false)
	def defaultMaxGroupSizeEnabled_=(isEnabled:Boolean) = settings += (Settings.DefaultMaxGroupSizeEnabled -> isEnabled)

	def defaultMaxGroupSize = getIntSetting(Settings.DefaultMaxGroupSize).getOrElse(DefaultGroupSize)
	def defaultMaxGroupSize_=(defaultSize:Int) = settings += (Settings.DefaultMaxGroupSize -> defaultSize)


	def toStringProps = Seq(
		"id" -> id,
		"name" -> name,
		"module" -> module)

  def duplicateTo( module:Module, assessmentGroups:JList[AssessmentGroup] = JArrayList()):SmallGroupSet = {
    val newSet = new SmallGroupSet()
    newSet.id = id
    newSet.academicYear = academicYear
    newSet.allocationMethod = allocationMethod
    newSet.allowSelfGroupSwitching = allowSelfGroupSwitching
    newSet.archived = archived
    newSet.assessmentGroups = assessmentGroups
    newSet.format = format
    newSet.groups = groups.asScala.map(_.duplicateTo(newSet)).asJava
    newSet._membersGroup = _membersGroup.duplicate()
    newSet.membershipService= membershipService
    newSet.module = module
    newSet.name = name
    newSet.permissionsService = permissionsService
    newSet.releasedToStudents = releasedToStudents
    newSet.releasedToTutors = releasedToTutors
		newSet.settings = Map() ++ settings
    newSet
  }

	def postLoad {
		ensureSettings
	}
}

