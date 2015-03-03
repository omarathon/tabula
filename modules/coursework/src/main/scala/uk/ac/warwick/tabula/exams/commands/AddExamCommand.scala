package uk.ac.warwick.tabula.exams.commands

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._

object AddExamCommand  {
	def apply(module: Module, academicYear1: AcademicYear) =
		new AddExamCommandInternal(module, academicYear1)
			with ComposableCommand[Exam]
			with AddExamPermissions
			with AddExamCommandState
			with AddExamCommandDescription
			with ExamValidation
			with UpdatesStudentMembership
			with AutowiringAssessmentServiceComponent
			with AutowiringAssessmentMembershipServiceComponent
			with HasAcademicYear
			with AutowiringUserLookupComponent
			with SpecifiesGroupType
			with ModifiesExamMembership {

			override def academicYear: AcademicYear = academicYear1
		}
}

class AddExamCommandInternal(val module: Module, val examAcademicYear: AcademicYear)
	extends CommandInternal[Exam]
	with AddExamCommandState
	with UpdatesStudentMembership
	with ModifiesExamMembership {

	self: AssessmentServiceComponent with UserLookupComponent  with HasAcademicYear with SpecifiesGroupType
	with AssessmentMembershipServiceComponent =>

	override def applyInternal() = {
		val exam = new Exam
		exam.name = name
		exam.module = module
		exam.academicYear = examAcademicYear

		exam.assessmentGroups.clear()
		exam.assessmentGroups.addAll(assessmentGroups)
		for (group <- exam.assessmentGroups if group.exam == null) {
			group.exam = exam
		}
		assessmentService.save(exam)
		exam
	}
}

trait AddExamPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AddExamCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Assignment.Create, module)
	}

}

trait ExamState {
	val updateStudentMembershipGroupIsUniversityIds:Boolean=false
	// bind variables
	var name: String = _
	def exam: Exam = null
}

trait AddExamCommandState extends ExamState {
	def module: Module
	def examAcademicYear: AcademicYear
}

trait AddExamCommandDescription extends Describable[Exam] {
	self: AddExamCommandState =>

	def describe(d: Description) {
		d.module(module)
	}
}

trait ExamValidation extends SelfValidating {

	self: ExamState =>

	override def validate(errors: Errors) {

		if (!name.hasText) {
			errors.rejectValue("name", "exam.name.empty")
		}
	}

}

trait ModifiesExamMembership extends UpdatesStudentMembership with SpecifiesGroupType {
	self: ExamState with HasAcademicYear with UserLookupComponent with AssessmentMembershipServiceComponent =>

	// start complicated membership stuff

	lazy val existingGroups: Option[Seq[UpstreamAssessmentGroup]] = Option(exam).map { _.upstreamAssessmentGroups }
	lazy val existingMembers: Option[UnspecifiedTypeUserGroup] = None //Option(exam).map { _.members }

	def updateAssessmentGroups() {
		assessmentGroups = upstreamGroups.asScala.flatMap ( ug => {
			val template = new AssessmentGroup
			template.assessmentComponent = ug.assessmentComponent
			template.occurrence = ug.occurrence
			template.exam = exam
			assessmentMembershipService.getAssessmentGroup(template) orElse Some(template)
		}).distinct.asJava
	}

	// end of complicated membership stuff
}
