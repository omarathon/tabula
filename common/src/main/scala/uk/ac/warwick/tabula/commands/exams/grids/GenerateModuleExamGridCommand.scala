package uk.ac.warwick.tabula.commands.exams.grids

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}


object GenerateModuleExamGridCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new GenerateModuleExamGridCommandInternal(department, academicYear)
			with AutowiringCourseAndRouteServiceComponent
			with AutowiringStudentCourseYearDetailsDaoComponent
			with AutowiringAssessmentMembershipServiceComponent
			with AutowiringModuleRegistrationServiceComponent
			with ComposableCommand[ModuleExamGridResult]
			with GenerateModuleExamGridValidation
			with GenerateModuleExamGridPermissions
			with GenerateModuleExamGridCommandState
			with GenerateModuleExamGridCommandRequest
			with ReadOnly with Unaudited
}

case class ModuleExamGridResult(
	upstreamAssessmentGroupAndSequenceAndOccurrencesWithComponentName: Seq[(String, String)],
	gridStudentDetailRecords: Seq[ModuleGridDetailRecord]
)

case class ModuleGridDetailRecord(
	moduleRegistration: ModuleRegistration,
	componentInfo: Map[String, AssessmentComponentInfo],
	name: String,
	universityId: String,
	lastImportDate: Option[DateTime]
)


case class AssessmentComponentInfo(mark: BigDecimal, grade: String, isActualMark: Boolean, isActualGrade: Boolean, resitInfo: ResitComponentInfo)

case class ResitComponentInfo(resitMark: BigDecimal, resitGrade: String, isActualResitMark: Boolean, isActualResitGrade: Boolean)

class GenerateModuleExamGridCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[ModuleExamGridResult] with TaskBenchmarking {

	self: StudentCourseYearDetailsDaoComponent with GenerateModuleExamGridCommandRequest with ModuleRegistrationServiceComponent with AssessmentMembershipServiceComponent =>

	override def applyInternal(): ModuleExamGridResult = {
		case class AssessmentIdentity(code: String, name: String)
		val result: Seq[(AssessmentIdentity, ModuleGridDetailRecord)] = benchmarkTask("GenerateMRComponents") {
			moduleRegistrationService.getByModuleAndYear(module, academicYear).flatMap { mr =>
				val student: StudentMember = mr.studentCourseDetails.student
				val componentInfo: Seq[(AssessmentIdentity, AssessmentComponentInfo)] = mr.upstreamAssessmentGroupMembers.flatMap { uagm =>
					val code = s"${uagm.upstreamAssessmentGroup.assessmentGroup}-${uagm.upstreamAssessmentGroup.sequence}-${uagm.upstreamAssessmentGroup.occurrence}"
					assessmentMembershipService.getAssessmentComponent(uagm.upstreamAssessmentGroup).filter(_.inUse).map { comp =>
						AssessmentIdentity(
							code = code,
							name = comp.name
						) -> AssessmentComponentInfo(
							mark = uagm.agreedMark.getOrElse(uagm.actualMark.orNull),
							grade = uagm.agreedGrade.getOrElse(uagm.actualGrade.orNull),
							isActualMark = uagm.agreedMark.isEmpty,
							isActualGrade = uagm.agreedGrade.isEmpty,
							resitInfo = ResitComponentInfo(
								resitMark = uagm.resitAgreedMark.getOrElse(uagm.resitActualMark.orNull),
								resitGrade = uagm.resitAgreedGrade.getOrElse(uagm.resitActualGrade.orNull),
								isActualResitMark = uagm.resitAgreedMark.isEmpty,
								isActualResitGrade = uagm.resitAgreedGrade.isEmpty
							)
						)
					}
				}

				componentInfo.map { case (assessmentIdentity, _) =>
					assessmentIdentity ->
						ModuleGridDetailRecord(
							moduleRegistration = mr,
							componentInfo = componentInfo.map { case (id, info) => (id.code, info) }.toMap,
							name = s"${student.firstName} ${student.lastName}",
							universityId = student.universityId,
							lastImportDate = Option(student.lastImportDate)
						)

				}
			}
		}

		ModuleExamGridResult(
			upstreamAssessmentGroupAndSequenceAndOccurrencesWithComponentName = result.toMap.keys.toSeq.map(assessmentIdentity =>
				(assessmentIdentity.code, assessmentIdentity.name)),
			gridStudentDetailRecords = result.map { case (_, records) => records }.distinct
		)
	}
}

trait GenerateModuleExamGridValidation extends SelfValidating {

	self: GenerateModuleExamGridCommandState with GenerateModuleExamGridCommandRequest =>
	override def validate(errors: Errors): Unit = {
		if (module == null) {
			errors.reject("examModuleGrid.module.empty")
		}
	}
}

trait GenerateModuleExamGridPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: GenerateModuleExamGridCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.ExamGrids, department)
	}

}

trait GenerateModuleExamGridCommandState {

	self: CourseAndRouteServiceComponent =>

	def department: Department

	def academicYear: AcademicYear

}

trait GenerateModuleExamGridCommandRequest {
	var module: Module = _
}
