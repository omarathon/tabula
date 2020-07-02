package uk.ac.warwick.tabula.helpers.marks

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, GradeBoundary, ModuleRegistration}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.AssessmentMembershipService

import scala.util.Try

object ValidGradesForMark {
  def getTuple(request: ModuleRegistrationValidGradesForMarkRequest, moduleRegistration: ModuleRegistration)(implicit assessmentMembershipService: AssessmentMembershipService): (Seq[GradeBoundary], Option[GradeBoundary]) = {
    val validGrades = request.mark.maybeText match {
      case Some(m) =>
        Try(m.toInt).toOption
          .map(asInt => assessmentMembershipService.gradesForMark(moduleRegistration, Some(asInt)))
          .getOrElse(Seq.empty)

      case None => assessmentMembershipService.gradesForMark(moduleRegistration, None)
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

  def getTuple(request: AssessmentComponentValidGradesForMarkRequest, assessmentComponent: AssessmentComponent)(implicit assessmentMembershipService: AssessmentMembershipService): (Seq[GradeBoundary], Option[GradeBoundary]) = {
    /*
     * Don't replace this with Option(resitAttempt)
     *
     * @ val jint: java.lang.Integer = null
     * jint: Integer = null
     * @ Option(jint)
     * res1: Option[Integer] = None
     * @ val sint: Option[Int] = Option(jint)
     * sint: Option[Int] = Some(0)
     * @ val sint2: Option[Int] = Option(jint).map(Int.unbox)
     * sint2: Option[Int] = None
     */
    val resitAttemptInt: Option[Int] = Option(request.resitAttempt).map(Int.unbox)

    val validGrades = request.mark.maybeText match {
      case Some(m) =>
        Try(m.toInt).toOption
          .map(asInt => assessmentMembershipService.gradesForMark(assessmentComponent, Some(asInt), resitAttemptInt))
          .getOrElse(Seq.empty)

      case None => assessmentMembershipService.gradesForMark(assessmentComponent, None, resitAttemptInt)
    }

    val default =
      if (request.existing.maybeText.nonEmpty && validGrades.exists(_.grade == request.existing)) {
        validGrades.find(_.grade == request.existing)
      } else {
        if (!assessmentComponent.module.adminDepartment.assignmentGradeValidationUseDefaultForZero && request.mark == "0") {
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
}

trait AssessmentComponentValidGradesForMarkRequest {
  var mark: String = _
  var existing: String = _
  var resitAttempt: JInteger = _
}
