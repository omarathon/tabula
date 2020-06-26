package uk.ac.warwick.tabula.helpers.marks

import uk.ac.warwick.tabula.JavaImports.JInteger
import uk.ac.warwick.tabula.data.model.{GradeBoundary, ModuleRegistration}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.AssessmentMembershipService

import scala.util.Try

object ValidGradesForMark {

  private def getResitAttempt(request: ModuleRegistrationValidGradesForMarkRequest): Option[Int] =
    if (request.resitAttempt != 0 && request.resitAttempt != null) Option(request.resitAttempt) else None

  def getTuple(request: ModuleRegistrationValidGradesForMarkRequest, moduleRegistration: ModuleRegistration)(implicit assessmentMembershipService: AssessmentMembershipService): (Seq[GradeBoundary], Option[GradeBoundary]) = {
    val validGrades = request.mark.maybeText match {
      case Some(m) =>
        Try(m.toInt).toOption
          .map(asInt => assessmentMembershipService.gradesForMark(moduleRegistration, Some(asInt), getResitAttempt(request)))
          .getOrElse(Seq.empty)

      case None => assessmentMembershipService.gradesForMark(moduleRegistration, None, getResitAttempt(request))
    }

    val default =
      if (request.existing.maybeText.nonEmpty && validGrades.exists(_.grade == request.existing)) {
        validGrades.find(_.grade == request.existing)
      } else {
        if (!moduleRegistration.module.adminDepartment.assignmentGradeValidationUseDefaultForZero && request.mark == "0") {
          None // TAB-3499
        } else {
          validGrades.find(_.isDefault)
        }
      }

    (validGrades, default)
  }
}

trait ModuleRegistrationValidGradesForMarkRequest {
  var mark: String = _
  var existing: String = _
  var resitAttempt: JInteger = _
}