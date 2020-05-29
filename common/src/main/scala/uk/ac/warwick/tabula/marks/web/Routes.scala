package uk.ac.warwick.tabula.marks.web

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, Department, UpstreamAssessmentGroup}
import uk.ac.warwick.tabula.web.RoutesUtils

/**
  * Generates URLs to various locations, to reduce the number of places where URLs
  * are hardcoded and repeated.
  *
  * For methods called "apply", you can leave out the "apply" and treat the object like a function.
  */
object Routes {

  import RoutesUtils._

  private val context = "/marks"

  def home: String = context + "/"

  object Admin {
    def home(department: Department): String = s"$context/admin/${encoded(department.code)}"
    def home(department: Department, academicYear: AcademicYear): String = s"$context/admin/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}"

    object AssessmentComponents {
      def apply(department: Department, academicYear: AcademicYear): String = s"$context/admin/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/assessment-components"
      def recordMarks(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup): String = s"$context/admin/assessment-component/${assessmentComponent.id}/${upstreamAssessmentGroup.id}/marks"
      def missingMarks(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup): String = s"$context/admin/assessment-component/${assessmentComponent.id}/${upstreamAssessmentGroup.id}/missing-marks"
      def scaling(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup): String = s"$context/admin/assessment-component/${assessmentComponent.id}/${upstreamAssessmentGroup.id}/scaling"
    }
  }
}
