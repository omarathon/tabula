package uk.ac.warwick.tabula.groups.commands.admin

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.StringUtils._

object ModifySmallGroupSetCommand {
	def create(module: Module) =
		new CreateSmallGroupSetCommandInternal(module)
			with ComposableCommand[SmallGroupSet]
			with ModifiesSmallGroupSetMembership
			with CreateSmallGroupSetPermissions
			with CreateSmallGroupSetDescription
			with ModifySmallGroupSetValidation
			with AutowiringSmallGroupServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringAssignmentMembershipServiceComponent

	def edit(module: Module, set: SmallGroupSet) =
		new EditSmallGroupSetCommandInternal(module, set)
			with ComposableCommand[SmallGroupSet]
			with ModifiesSmallGroupSetMembership
			with SmallGroupAutoDeregistration
			with EditSmallGroupSetPermissions
			with EditSmallGroupSetDescription
			with ModifySmallGroupSetValidation
			with AutowiringSmallGroupServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringAssignmentMembershipServiceComponent
}

trait ModifiesSmallGroupSetMembership extends UpdatesStudentMembership with SpecifiesGroupType {
	self: ModifySmallGroupSetCommandState with UserLookupComponent with AssignmentMembershipServiceComponent =>

	// start complicated membership stuff

	lazy val existingGroups: Option[Seq[UpstreamAssessmentGroup]] = existingSet.map(_.upstreamAssessmentGroups)
	lazy val existingMembers: Option[UnspecifiedTypeUserGroup] = existingSet.map(_.members)

	def copyGroupsFrom(smallGroupSet: SmallGroupSet) {
		upstreamGroups.addAll(availableUpstreamGroups.filter { ug =>
			assessmentGroups.asScala.exists( ag => ug.assessmentComponent == ag.assessmentComponent && ag.occurrence == ug.occurrence )
		}.asJavaCollection)
	}

	/**
	 * Convert Spring-bound upstream group references to an AssessmentGroup buffer
	 */
	def updateAssessmentGroups() {
		assessmentGroups = upstreamGroups.asScala.flatMap ( ug => {
			val template = new AssessmentGroup
			template.assessmentComponent = ug.assessmentComponent
			template.occurrence = ug.occurrence
			template.smallGroupSet = existingSet.orNull
			assignmentMembershipService.getAssessmentGroup(template) orElse Some(template)
		}).distinct.asJava
	}

	// end of complicated membership stuff
}

trait ModifySmallGroupSetCommandState extends CurrentAcademicYear {
	def module: Module
	def existingSet: Option[SmallGroupSet]

	var name: String = _

	var format: SmallGroupFormat = _

	var allocationMethod: SmallGroupAllocationMethod = SmallGroupAllocationMethod.Manual

	var allowSelfGroupSwitching: Boolean = true
	var studentsCanSeeTutorName: Boolean = false
	var studentsCanSeeOtherMembers: Boolean = false
	var defaultMaxGroupSizeEnabled: Boolean = false
	var defaultMaxGroupSize: Int = SmallGroup.DefaultGroupSize

	var collectAttendance: Boolean = true

	var linkedDepartmentSmallGroupSet: DepartmentSmallGroupSet = _
}

trait CreateSmallGroupSetCommandState extends ModifySmallGroupSetCommandState {
	self: UserLookupComponent with AssignmentMembershipServiceComponent =>

	val existingSet = None
}

trait EditSmallGroupSetCommandState extends ModifySmallGroupSetCommandState {
	self: UserLookupComponent with AssignmentMembershipServiceComponent =>

	def set: SmallGroupSet
	def existingSet = Some(set)
}

class CreateSmallGroupSetCommandInternal(val module: Module) extends ModifySmallGroupSetCommandInternal with CreateSmallGroupSetCommandState {
	self: SmallGroupServiceComponent with ModifiesSmallGroupSetMembership with UserLookupComponent with AssignmentMembershipServiceComponent =>

	override def applyInternal() = transactional() {
		val set = new SmallGroupSet(module)
		copyTo(set)
		smallGroupService.saveOrUpdate(set)
		set
	}
}

class EditSmallGroupSetCommandInternal(val module: Module, val set: SmallGroupSet) extends ModifySmallGroupSetCommandInternal with EditSmallGroupSetCommandState {
	self: SmallGroupServiceComponent with SmallGroupAutoDeregistration with ModifiesSmallGroupSetMembership with UserLookupComponent with AssignmentMembershipServiceComponent =>

	override def applyInternal() = transactional() {
		if (module.department.autoGroupDeregistration) {
			autoDeregister(set) { () =>
				copyTo(set)
				set
			}
		} else {
			copyTo(set)
		}

		smallGroupService.saveOrUpdate(set)
		set
	}
}

trait SmallGroupAutoDeregistration {
	self: AssignmentMembershipServiceComponent with ModifySmallGroupSetCommandState with ModifiesSmallGroupSetMembership =>

	def autoDeregister(set: SmallGroupSet)(fn: () => SmallGroupSet) = {
		val oldUsers =
			assignmentMembershipService.determineMembershipUsers(set.upstreamAssessmentGroups, Option(set.members)).toSet

		val newUsers =
			assignmentMembershipService.determineMembershipUsers(linkedUpstreamAssessmentGroups, Option(members)).toSet

		val updatedSet = fn()

		// Wrap removal in a sub-command so that we can do auditing
		for {
			user <- oldUsers -- newUsers
			group <- updatedSet.groups.asScala
			if (group.students.includesUser(user))
		} ??? // TODO FIXME removeFromGroupCommand(user, group).apply()

		updatedSet
	}
}

abstract class ModifySmallGroupSetCommandInternal(val updateStudentMembershipGroupIsUniversityIds: Boolean = true) extends CommandInternal[SmallGroupSet] with ModifySmallGroupSetCommandState {
	self: SmallGroupServiceComponent with ModifiesSmallGroupSetMembership =>

	def copyFrom(set: SmallGroupSet) {
		name = set.name
		academicYear = set.academicYear
		format = set.format
		allocationMethod = set.allocationMethod
		allowSelfGroupSwitching = set.allowSelfGroupSwitching
		studentsCanSeeTutorName = set.studentsCanSeeTutorName
		studentsCanSeeOtherMembers = set.studentsCanSeeOtherMembers
		defaultMaxGroupSizeEnabled = set.defaultMaxGroupSizeEnabled
		defaultMaxGroupSize = set.defaultMaxGroupSize
		collectAttendance = set.collectAttendance

		// linked assessmentGroups
		assessmentGroups = set.assessmentGroups

		linkedDepartmentSmallGroupSet = set.linkedDepartmentSmallGroupSet

		if (set.members != null) members = set.members.duplicate()
	}

	def copyTo(set: SmallGroupSet) {
		set.name = name
		set.academicYear = academicYear
		set.format = format
		set.allocationMethod = allocationMethod

		set.collectAttendance = collectAttendance

		set.assessmentGroups.clear
		set.assessmentGroups.addAll(assessmentGroups)
		for (group <- set.assessmentGroups.asScala if group.smallGroupSet == null) {
			group.smallGroupSet = set
		}

		set.allowSelfGroupSwitching = allowSelfGroupSwitching
		set.studentsCanSeeOtherMembers = studentsCanSeeOtherMembers
		set.studentsCanSeeTutorName = studentsCanSeeTutorName
		set.defaultMaxGroupSizeEnabled = defaultMaxGroupSizeEnabled
		set.defaultMaxGroupSize = defaultMaxGroupSize

		set.linkedDepartmentSmallGroupSet = linkedDepartmentSmallGroupSet

		if (set.members == null) set.members = UserGroup.ofUniversityIds
		set.members.copyFrom(members)
	}
}

trait ModifySmallGroupSetValidation extends SelfValidating {
	self: ModifySmallGroupSetCommandState =>

	override def validate(errors: Errors) {
		if (!name.hasText) errors.rejectValue("name", "smallGroupSet.name.NotEmpty")
		else if (name.orEmpty.length > 200) errors.rejectValue("name", "smallGroupSet.name.Length", Array[Object](200: JInteger), "")

		if (format == null) errors.rejectValue("format", "smallGroupSet.format.NotEmpty")
		if (allocationMethod == null) errors.rejectValue("allocationMethod", "smallGroupSet.allocationMethod.NotEmpty")
	}
}

trait CreateSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: CreateSmallGroupSetCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.Create, mandatory(module))
	}
}

trait CreateSmallGroupSetDescription extends Describable[SmallGroupSet] {
	self: CreateSmallGroupSetCommandState =>

	override def describe(d: Description) {
		d.module(module).properties("name" -> name)
	}

	override def describeResult(d: Description, set: SmallGroupSet) =
		d.smallGroupSet(set)
}

trait EditSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditSmallGroupSetCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, module)
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
	}
}

trait EditSmallGroupSetDescription extends Describable[SmallGroupSet] {
	self: EditSmallGroupSetCommandState =>

	override def describe(d: Description) {
		d.smallGroupSet(set)
	}

}