package uk.ac.warwick.tabula.commands.marks

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Describable, Description}
import uk.ac.warwick.tabula.commands.marks.CalculateModuleMarksCommand.Result
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.StudentMarkRecord
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand.StudentModuleMarkRecord
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, Module, ModuleRegistration}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, ModuleRegistrationServiceComponent}
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, ModuleRegistrationMarksServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

// Traits for commands that act on a specific run of a module - extend as required

trait ModuleOccurrenceState {
  def module: Module
  def cats: BigDecimal
  def academicYear: AcademicYear
  def occurrence: String
}

trait ModuleOccurrenceLoadModuleRegistrations {
  self: ModuleOccurrenceState
    with AssessmentMembershipServiceComponent
    with AssessmentComponentMarksServiceComponent
    with ModuleRegistrationServiceComponent
    with ModuleRegistrationMarksServiceComponent =>

  lazy val assessmentComponents: Seq[AssessmentComponent] =
    assessmentMembershipService.getAssessmentComponents(module)
      .filter { ac =>
        ac.cats.map(BigDecimal(_).setScale(1, BigDecimal.RoundingMode.HALF_UP)).contains(cats.setScale(1, BigDecimal.RoundingMode.HALF_UP)) &&
          ac.assessmentGroup != "AO" &&
          ac.sequence != AssessmentComponent.NoneAssessmentGroup
      }

  lazy val studentComponentMarkRecords: Seq[(AssessmentComponent, Seq[StudentMarkRecord])] =
    assessmentMembershipService.getUpstreamAssessmentGroupInfoForComponents(assessmentComponents, academicYear)
      .filter { info =>
        info.upstreamAssessmentGroup.occurrence == occurrence &&
          info.allMembers.nonEmpty
      }
      .map { info =>
        info.upstreamAssessmentGroup.assessmentComponent.get -> ListAssessmentComponentsCommand.studentMarkRecords(info, assessmentComponentMarksService)
      }

  def componentMarks(universityId: String): Map[AssessmentComponent, StudentMarkRecord] = studentComponentMarkRecords
    .filter(_._2.exists(_.universityId == universityId))
    .map { case (ac, allStudents) => ac -> allStudents.find(_.universityId == universityId).get }
    .toMap

  lazy val moduleRegistrations: Seq[ModuleRegistration] = moduleRegistrationService.getByModuleOccurrence(module, cats, academicYear, occurrence)

  lazy val studentModuleMarkRecords: Seq[StudentModuleMarkRecord] =
    MarksDepartmentHomeCommand.studentModuleMarkRecords(module, cats, academicYear, occurrence, moduleRegistrations, moduleRegistrationMarksService)
}

trait ModuleOccurrenceUpdateMarksPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ModuleOccurrenceState with ModuleOccurrenceLoadModuleRegistrations =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Feedback.Publish, mandatory(module))

    // Make sure that the module occurrence actually exists
    mandatory(cats)
    mandatory(academicYear)
    mandatory(occurrence)
    mandatory(moduleRegistrations.headOption)
  }
}

trait ModuleOccurrenceDescription extends Describable[Result] {
  self: ModuleOccurrenceState =>

  def mandatoryEventName: String

  override lazy val eventName: String = mandatoryEventName

  override def describe(d: Description): Unit =
    d.module(module)
      .properties(
        "cats" -> cats.setScale(1, BigDecimal.RoundingMode.HALF_UP).toString,
        "academicYear" -> academicYear.toString,
        "occurrence" -> occurrence,
      )

  override def describeResult(d: Description, result: Result): Unit =
    d.properties(
      "marks" -> result.filter(_.latestMark.nonEmpty).map { student =>
        student.sprCode -> student.latestMark.get
      }.toMap,
      "grades" -> result.filter(_.latestGrade.nonEmpty).map { student =>
        student.sprCode -> student.latestGrade.get
      }.toMap,
      "results" -> result.filter(_.latestResult.nonEmpty).map { student =>
        student.sprCode -> student.latestResult.get.entryName
      }.toMap,
      "state" -> result.filter(_.latestState.nonEmpty).map { student =>
        student.sprCode -> student.latestState.get.entryName
      }.toMap
    )
}
