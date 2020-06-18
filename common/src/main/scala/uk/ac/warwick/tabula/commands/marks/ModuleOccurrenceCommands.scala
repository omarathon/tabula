package uk.ac.warwick.tabula.commands.marks

import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}
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
  def sitsModuleCode: String
  def module: Module
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
    assessmentMembershipService.getAssessmentComponents(sitsModuleCode, inUseOnly = false)
      .filter { ac =>
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

  def componentMarks(universityId: String): Map[AssessmentComponent, StudentMarkRecord] = {
    // Find the assessment group to filter by (this is for students who take multiple reassessments)
    val assessmentGroup =
      studentComponentMarkRecords
        .flatMap(_._2.find(_.universityId == universityId))
        .maxByOption(_.resitSequence)
        .map(_.upstreamAssessmentGroupMember.upstreamAssessmentGroup.assessmentGroup)

    studentComponentMarkRecords
      .filter { case (ac, allStudents) => assessmentGroup.contains(ac.assessmentGroup) && allStudents.exists(_.universityId == universityId) }
      .map { case (ac, allStudents) =>
        ac -> allStudents.filter(_.universityId == universityId).sortBy(_.resitSequence).reverse.head // Resits first, then latest by resit sequence
      }
      .toMap
  }

  lazy val moduleRegistrations: Seq[ModuleRegistration] = moduleRegistrationService.getByModuleOccurrence(sitsModuleCode, academicYear, occurrence)

  lazy val studentModuleMarkRecords: Seq[StudentModuleMarkRecord] =
    MarksDepartmentHomeCommand.studentModuleMarkRecords(sitsModuleCode, academicYear, occurrence, moduleRegistrations, moduleRegistrationMarksService)
}

trait ModuleOccurrenceUpdateMarksPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ModuleOccurrenceState with ModuleOccurrenceLoadModuleRegistrations =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Feedback.Publish, mandatory(module))

    // Make sure sitsModuleCode is for the same module
    if (mandatory(Module.stripCats(mandatory(sitsModuleCode))).toLowerCase != mandatory(module.code)) {
      logger.info("Not displaying module as it doesn't match SITS module code")
      throw new ItemNotFoundException(module, "Not displaying module as it doesn't match SITS module code")
    }

    // Make sure that the module occurrence actually exists
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
        "sitsModuleCode" -> sitsModuleCode,
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
