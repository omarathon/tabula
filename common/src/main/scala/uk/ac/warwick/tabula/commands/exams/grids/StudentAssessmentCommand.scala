package uk.ac.warwick.tabula.commands.exams.grids

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringStudentCourseYearDetailsDaoComponent, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.exams.grids.{AutowiringNormalCATSLoadServiceComponent, NormalCATSLoadServiceComponent, NormalLoadLookup}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object StudentAssessmentCommand {
	def apply(studentCourseYearDetails: StudentCourseYearDetails, academicYear: AcademicYear) =
		new StudentAssessmentCommandInternal(studentCourseYearDetails, academicYear)
			with AutowiringCourseAndRouteServiceComponent
			with AutowiringStudentCourseYearDetailsDaoComponent
			with AutowiringAssessmentMembershipServiceComponent
			with AutowiringNormalCATSLoadServiceComponent
			with ComposableCommand[Seq[GridAssessmentComponentDetails]]
			with StudentAssessmentPermissions
			with StudentAssessmentCommandState
			with ReadOnly with Unaudited
}


case class GridAssessmentComponentDetails(
	moduleRegistration: ModuleRegistration,
	studentAssessmentComponentInfo: Seq[StudentAssessmentComponentInfo]
)


case class StudentAssessmentComponentInfo(grpWithComponentInfo: UpstreamGroup, groupMember: UpstreamAssessmentGroupMember)


class StudentAssessmentCommandInternal(val studentCourseYearDetails: StudentCourseYearDetails, val academicYear: AcademicYear)
	extends CommandInternal[Seq[GridAssessmentComponentDetails]] with TaskBenchmarking {

	self: StudentCourseYearDetailsDaoComponent with StudentCourseYearDetailsDaoComponent with AssessmentMembershipServiceComponent =>
	//with GenerateExamGridSelectCourseCommandRequest =>

	override def applyInternal(): Seq[GridAssessmentComponentDetails] = {
		studentCourseYearDetails.moduleRegistrations.map { mr =>
			val studentAssessmentInfo = for {
				uagm <- mr.upstreamAssessmentGroupMembers
				aComponent <- assessmentMembershipService.getAssessmentComponent(uagm.upstreamAssessmentGroup)
			} yield StudentAssessmentComponentInfo(new UpstreamGroup(aComponent, uagm.upstreamAssessmentGroup), uagm)
			GridAssessmentComponentDetails(mr, studentAssessmentInfo)
		}
	}
}


trait StudentAssessmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: StudentAssessmentCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.ExamGrids, studentCourseYearDetails.studentCourseDetails.department)
	}

}


trait StudentAssessmentCommandState {
	self: StudentCourseYearDetailsDaoComponent with NormalCATSLoadServiceComponent =>

	def academicYear: AcademicYear

	def studentCourseYearDetails: StudentCourseYearDetails

	lazy val normalLoadLookup: NormalLoadLookup = new NormalLoadLookup(academicYear, studentCourseYearDetails.yearOfStudy, normalCATSLoadService)
}
