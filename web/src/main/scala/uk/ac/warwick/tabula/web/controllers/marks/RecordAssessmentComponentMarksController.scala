package uk.ac.warwick.tabula.web.controllers.marks

import javax.validation.Valid
import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, PostMapping, RequestMapping, RequestParam}
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.StudentMarkRecord
import uk.ac.warwick.tabula.commands.marks.RecordAssessmentComponentMarksCommand.StudentMarksItem
import uk.ac.warwick.tabula.commands.marks.{ListAssessmentComponentsCommand, RecordAssessmentComponentMarksCommand}
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, UpstreamAssessmentGroup, UpstreamAssessmentGroupInfo, UpstreamAssessmentGroupMemberAssessmentType}
import uk.ac.warwick.tabula.jobs.scheduling.ImportMembersJob
import uk.ac.warwick.tabula.services.jobs.AutowiringJobServiceComponent
import uk.ac.warwick.tabula.services.marks.{AutowiringAssessmentComponentMarksServiceComponent, AutowiringResitServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringAssessmentMembershipServiceComponent, AutowiringMaintenanceModeServiceComponent, AutowiringProfileServiceComponent}
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.web.{BreadCrumb, Mav, Routes}

import scala.jdk.CollectionConverters._

@Controller
@RequestMapping(Array("/marks/admin/assessment-component/{assessmentComponent}/{upstreamAssessmentGroup}/marks"))
class RecordAssessmentComponentMarksController extends BaseComponentMarksController
  with AutowiringAssessmentComponentMarksServiceComponent
  with AutowiringAssessmentMembershipServiceComponent
  with AutowiringProfileServiceComponent
  with AutowiringJobServiceComponent
  with AutowiringMaintenanceModeServiceComponent
  with AutowiringResitServiceComponent {

  @ModelAttribute("command")
  def command(@PathVariable assessmentComponent: AssessmentComponent, @PathVariable upstreamAssessmentGroup: UpstreamAssessmentGroup): RecordAssessmentComponentMarksCommand.Command =
    RecordAssessmentComponentMarksCommand(assessmentComponent, upstreamAssessmentGroup, user)

  @ModelAttribute("breadcrumbs")
  def breadcrumbs(@PathVariable assessmentComponent: AssessmentComponent, @PathVariable upstreamAssessmentGroup: UpstreamAssessmentGroup): Seq[BreadCrumb] = {
    val department = assessmentComponent.module.adminDepartment
    val academicYear = upstreamAssessmentGroup.academicYear

    Seq(
      MarksBreadcrumbs.Admin.HomeForYear(department, academicYear),
      MarksBreadcrumbs.Admin.AssessmentComponents(department, academicYear),
      MarksBreadcrumbs.Admin.AssessmentComponentRecordMarks(assessmentComponent, upstreamAssessmentGroup, active = true),
    )
  }

  @ModelAttribute("studentMarkRecords")
  def studentMarkRecords(@PathVariable upstreamAssessmentGroup: UpstreamAssessmentGroup): Seq[StudentMarkRecord] = {
    val info = UpstreamAssessmentGroupInfo(
      upstreamAssessmentGroup,
      assessmentMembershipService.getCurrentUpstreamAssessmentGroupMembers(upstreamAssessmentGroup.id)
    )

    ListAssessmentComponentsCommand.studentMarkRecords(info, assessmentComponentMarksService, resitService, assessmentMembershipService)
  }

  @ModelAttribute("isGradeValidation")
  def isGradeValidation(@PathVariable assessmentComponent: AssessmentComponent): Boolean =
    assessmentComponent.module.adminDepartment.assignmentGradeValidation

  private val formView: String = "marks/admin/assessment-components/record"

  /**
   * Upon loading the form, trigger a skippable import if:
   *
   * - Any of the student mark records is outOfSync; or
   * - The most recent import date was more than 5 minutes ago
   */
  @RequestMapping
  def triggerImportIfNecessary(
    @ModelAttribute("studentMarkRecords") studentMarkRecords: Seq[StudentMarkRecord],
    @PathVariable upstreamAssessmentGroup: UpstreamAssessmentGroup,
    model: ModelMap,
    @ModelAttribute("command") command: RecordAssessmentComponentMarksCommand.Command,
  ): String = {
    val universityIds = studentMarkRecords.map(_.universityId)
    lazy val members = profileService.getAllMembersWithUniversityIds(universityIds)

    if (!maintenanceModeService.enabled && (studentMarkRecords.exists(_.outOfSync) || members.flatMap(m => Option(m.lastImportDate)).exists(_.isBefore(DateTime.now.minusMinutes(5))))) {
      stopOngoingImportForStudents(universityIds)

      val studentLastImportDates =
        members.map(m =>
          (m.fullName.getOrElse(m.universityId), Option(m.lastImportDate).getOrElse(new DateTime(0)))
        ).sortBy(_._2)

      val jobInstance = jobService.add(Some(user), ImportMembersJob(universityIds, Seq(upstreamAssessmentGroup.academicYear)))

      model.addAttribute("jobId", jobInstance.id)
      model.addAttribute("jobProgress", jobInstance.progress)
      model.addAttribute("jobStatus", jobInstance.status)
      model.addAttribute("oldestImport", studentLastImportDates.headOption.map { case (_, datetime) => datetime })
      model.addAttribute("studentLastImportDates", studentLastImportDates)

      "marks/admin/assessment-components/job-progress"
    } else {
      command.populate()
      formView
    }
  }

  private def stopOngoingImportForStudents(universityIds: Seq[String]): Unit =
    jobService.jobDao.listRunningJobs
      .filter(_.jobType == ImportMembersJob.identifier)
      .filter(_.getStrings(ImportMembersJob.MembersKey).toSet == universityIds.toSet)
      .foreach(jobService.kill)

  @PostMapping(Array("/progress"))
  def jobProgress(@RequestParam jobId: String): Mav =
    jobService.getInstance(jobId).map(jobInstance =>
      Mav(new JSONView(Map(
        "id" -> jobInstance.id,
        "status" -> jobInstance.status,
        "progress" -> jobInstance.progress,
        "finished" -> jobInstance.finished
      ))).noLayout()
    ).getOrElse(throw new ItemNotFoundException)

  // For people who hit the back button back to the /skip-import page or refresh it
  @RequestMapping(Array("/skip-import"))
  def skipImportRefresh(@ModelAttribute("command") command: RecordAssessmentComponentMarksCommand.Command): String = {
    command.populate()
    formView
  }

  @PostMapping(Array("/skip-import"))
  def skipImport(@RequestParam jobId: String, @ModelAttribute("command") command: RecordAssessmentComponentMarksCommand.Command): String = {
    jobService.getInstance(jobId)
      .filterNot(_.finished)
      .filter(_.jobType == ImportMembersJob.identifier)
      .foreach(jobService.kill)

    command.populate()
    formView
  }

  @RequestMapping(Array("/import-complete"))
  def importComplete(@ModelAttribute("command") command: RecordAssessmentComponentMarksCommand.Command): String = {
    command.populate()
    formView
  }

  @PostMapping(params = Array("!confirm"))
  def preview(
    @Valid @ModelAttribute("command") cmd: RecordAssessmentComponentMarksCommand.Command,
    errors: Errors,
    model: ModelMap,
    @ModelAttribute("studentMarkRecords") studentMarkRecords: Seq[StudentMarkRecord],
    @PathVariable assessmentComponent: AssessmentComponent,
    @PathVariable upstreamAssessmentGroup: UpstreamAssessmentGroup,
  ): String =
    if (errors.hasErrors) {
      if (errors.hasFieldErrors("file")) {
        model.addAttribute("from_origin", "upload")
      } else {
        model.addAttribute("from_origin", "webform")
      }
      model.addAttribute("flash__error", "flash.hasErrors")
      formView
    } else {
      // Filter down to just changes
      val changes: Seq[(StudentMarkRecord, StudentMarksItem)] =
        cmd.students.asScala.values.toSeq.flatMap { student =>
          // We know the .get is safe because it's validated
          val studentMarkRecord = studentMarkRecords.find { m =>
            m.universityId == student.universityID && (
              (student.resitSequence.maybeText.isEmpty && m.upstreamAssessmentGroupMember.assessmentType == UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment) ||
              (m.upstreamAssessmentGroupMember.assessmentType == UpstreamAssessmentGroupMemberAssessmentType.Reassessment && m.resitSequence.contains(student.resitSequence))
            )
          }.get

          // Mark and grade haven't changed and no comment and not out of sync (we always re-push out of sync records)
          if (
            !studentMarkRecord.outOfSync &&
            !student.comments.hasText &&
            ((!student.mark.hasText && studentMarkRecord.mark.isEmpty) || studentMarkRecord.mark.map(_.toString).contains(student.mark)) &&
            ((!student.grade.hasText && studentMarkRecord.grade.isEmpty) || studentMarkRecord.grade.contains(student.grade))
          ) None else Some(studentMarkRecord -> student)
        }

      model.addAttribute("changes", changes)

      "marks/admin/assessment-components/record_preview"
    }

  @PostMapping(params = Array("confirm=true", "action=Confirm"))
  def save(
    @Valid @ModelAttribute("command") cmd: RecordAssessmentComponentMarksCommand.Command,
    errors: Errors,
    @PathVariable assessmentComponent: AssessmentComponent,
    @PathVariable upstreamAssessmentGroup: UpstreamAssessmentGroup,
    model: ModelMap,
  )(implicit redirectAttributes: RedirectAttributes): String =
    if (errors.hasErrors) {
      model.addAttribute("from_origin", "webform")
      model.addAttribute("flash__error", "flash.hasErrors")
      formView
    } else {
      cmd.apply()

      RedirectFlashing(Routes.marks.Admin.home(assessmentComponent.module.adminDepartment, upstreamAssessmentGroup.academicYear), "flash__success" -> "flash.assessmentComponent.marksRecorded")
    }

  @PostMapping(params = Array("confirm=true", "action!=Confirm"))
  def cancelPreview(
    @Valid @ModelAttribute("command") cmd: RecordAssessmentComponentMarksCommand.Command,
    errors: Errors,
    @PathVariable assessmentComponent: AssessmentComponent,
    @PathVariable upstreamAssessmentGroup: UpstreamAssessmentGroup
  ): String = {
    cmd.populate()
    formView
  }


}
