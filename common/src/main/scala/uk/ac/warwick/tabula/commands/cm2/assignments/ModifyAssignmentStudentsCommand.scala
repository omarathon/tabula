package uk.ac.warwick.tabula.commands.cm2.assignments

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports.JBoolean
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object ModifyAssignmentStudentsCommand {
	def apply(assignment: Assignment) =
		new ModifyAssignmentStudentsCommandInternal(assignment)
			with ComposableCommand[Assignment]
			with AutowiringUserLookupComponent
			with ModifyAssignmentStudentsPermissions
			with ModifyAssignmentStudentsDescription
			with ModifyAssignmentStudentsCommandState
			with ModifyAssignmentStudentsValidation
			with AutowiringAssessmentServiceComponent
			with AutowiringAssessmentMembershipServiceComponent
			with CurrentSITSAcademicYear
			with ModifiesAssignmentMembership
			with SharedAssignmentStudentProperties {
			copyMembers(assignment)
		}
}

class ModifyAssignmentStudentsCommandInternal(override val assignment: Assignment)
	extends CommandInternal[Assignment] with PopulateOnForm {

	self: AssessmentServiceComponent with UserLookupComponent
		with AssessmentMembershipServiceComponent with ModifyAssignmentStudentsCommandState
		with SharedAssignmentStudentProperties with ModifiesAssignmentMembership =>


	override def applyInternal(): Assignment = {
		this.copyTo(assignment)
		assessmentService.save(assignment)
		assignment
	}

	override def populate(): Unit = {
		copySharedStudentFrom(assignment)
		assessmentGroups = assignment.assessmentGroups
		upstreamGroups.addAll(allUpstreamGroups.filter { ug =>
			assessmentGroups.asScala.exists(ag => ug.assessmentComponent == ag.assessmentComponent && ag.occurrence == ug.occurrence)
		}.asJavaCollection)
	}

}


trait ModifyAssignmentStudentsCommandState extends EditAssignmentMembershipCommandState with UpdatesStudentMembership {
	self: AssessmentServiceComponent with UserLookupComponent with SpecifiesGroupType with SharedAssignmentStudentProperties
		with AssessmentMembershipServiceComponent =>

	val updateStudentMembershipGroupIsUniversityIds: Boolean = false

	def copyTo(assignment: Assignment) {
		copySharedStudentTo(assignment)
		assignment.assessmentGroups.clear()
		assignment.assessmentGroups.addAll(assessmentGroups)

		for (group <- assignment.assessmentGroups.asScala if group.assignment == null) {
			group.assignment = assignment
		}
		assignment.members.copyFrom(members)
	}

}


trait ModifyAssignmentStudentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ModifyAssignmentStudentsCommandState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		notDeleted(assignment)
		p.PermissionCheck(Permissions.Assignment.Update, assignment.module)
	}
}


trait ModifyAssignmentStudentsDescription extends Describable[Assignment] {
	self: ModifyAssignmentStudentsCommandState =>

	override lazy val eventName: String = "ModifyAssignmentStudents"

	override def describe(d: Description) {
		d.assignment(assignment)
	}
}

trait ModifyAssignmentStudentsValidation extends SelfValidating {

	self: ModifyAssignmentStudentsCommandState with AssessmentServiceComponent with UserLookupComponent with ModifiesAssignmentMembership =>

	override def validate(errors: Errors) {

		def isValidUniID(userString: String) = {
			UniversityId.isValid(userString) && userLookup.getUserByWarwickUniId(userString).isFoundUser
		}

		def isValidUserCode(userString: String) = {
			val user = userLookup.getUserByUserId(userString)
			user.isFoundUser && null != user.getWarwickId
		}

		val invalidUserStrings = massAddUsersEntries.filterNot(userString => isValidUniID(userString) || isValidUserCode(userString))
		if (invalidUserStrings.nonEmpty) {
			errors.rejectValue("massAddUsers", "userString.notfound.specified", Array(invalidUserStrings.mkString(", ")), "")
		}
	}
}

