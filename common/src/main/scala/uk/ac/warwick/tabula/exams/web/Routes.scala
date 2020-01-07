package uk.ac.warwick.tabula.exams.web

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.jobs.JobInstance

/**
  * Generates URLs to various locations, to reduce the number of places where URLs
  * are hardcoded and repeated.
  *
  * For methods called "apply", you can leave out the "apply" and treat the object like a function.
  */
object Routes {

  import uk.ac.warwick.tabula.web.RoutesUtils._

  private val context = "/exams"

  def home: String = context + "/"

  object Grids {

    private val context = "/exams/grids"

    def home: String = context + "/"

    def departmentAcademicYear(department: Department, academicYear: AcademicYear): String =
      s"$context/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}"

    def generate(department: Department, academicYear: AcademicYear): String =
      s"$context/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/generate"

    def options(department: Department, academicYear: AcademicYear): String =
      s"$context/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/generate/options"

    def coreRequired(department: Department, academicYear: AcademicYear): String =
      s"$context/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/generate/corerequired"

    def jobProgress(department: Department, academicYear: AcademicYear): String =
      s"$context/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/generate/import"

    def jobSkip(department: Department, academicYear: AcademicYear): String =
      s"$context/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/generate/import/skip"

    def preview(department: Department, academicYear: AcademicYear): String =
      s"$context/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/generate/preview"

    def moduleGenerate(department: Department, academicYear: AcademicYear): String =
      s"$context/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/module/generate"

    def moduleJobProgress(department: Department, academicYear: AcademicYear): String =
      s"$context/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/module/generate/import"

    def documentProgress(department: Department, academicYear: AcademicYear, jobInstance: JobInstance): String =
      s"$context/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/generate/documents/${jobInstance.id}/progress?clearModel=true"

    def document(department: Department, academicYear: AcademicYear, jobInstance: JobInstance): String =
      s"$context/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/generate/documents/${jobInstance.id}/download"

    def moduleJobSkip(department: Department, academicYear: AcademicYear): String =
      s"$context/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/module/generate/import/skip"

    def modulePreview(department: Department, academicYear: AcademicYear): String =
      s"$context/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/module/generate/preview"

    def normalLoad(department: Department, academicYear: AcademicYear): String =
      s"$context/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/normalload"

    def weightings(department: Department, academicYear: AcademicYear): String =
      s"$context/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/weightings"

    def assessmentdetails(scyd: StudentCourseYearDetails): String =
      s"$context/${encoded(scyd.enrolmentDepartment.code)}/${encoded(scyd.academicYear.value.toString)}/${encoded(scyd.studentCourseDetails.urlSafeId)}/assessmentdetails"
  }

}
