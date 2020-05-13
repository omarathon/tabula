package uk.ac.warwick.tabula.web.controllers.marks

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, PostMapping, RequestMapping}
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.marks.RecordAssessmentComponentMarksCommand
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, UpstreamAssessmentGroup, UpstreamAssessmentGroupInfo}
import uk.ac.warwick.tabula.services.AutowiringAssessmentMembershipServiceComponent
import uk.ac.warwick.tabula.services.marks.AutowiringAssessmentComponentMarksServiceComponent
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.controllers.marks.RecordAssessmentComponentMarksController.StudentMarkRecord
import uk.ac.warwick.tabula.web.{BreadCrumb, Routes}

object RecordAssessmentComponentMarksController {
  case class StudentMarkRecord(
    universityId: String,
    position: Option[Int],
    currentMember: Boolean,
    mark: Option[Int],
    grade: Option[String],
    needsWritingToSits: Boolean,
    outOfSync: Boolean,
    agreed: Boolean
  )
}

@Controller
@RequestMapping(Array("/marks/admin/assessment-component/{assessmentComponent}/{upstreamAssessmentGroup}/marks"))
class RecordAssessmentComponentMarksController extends BaseController
  with AutowiringAssessmentComponentMarksServiceComponent
  with AutowiringAssessmentMembershipServiceComponent {

  validatesSelf[SelfValidating]

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

    val recordedStudents = assessmentComponentMarksService.getAllRecordedStudents(upstreamAssessmentGroup)

    info.allMembers.map { member =>
      val recordedStudent = recordedStudents.find(_.universityId == member.universityId)

      StudentMarkRecord(
        universityId = member.universityId,
        position = member.position,
        currentMember = info.currentMembers.contains(member),
        mark =
          recordedStudent.filter(_.needsWritingToSits).flatMap(_.latestMark)
            .orElse(member.firstAgreedMark.map(_.toInt))
            .orElse(recordedStudent.flatMap(_.latestMark))
            .orElse(member.firstDefinedMark.map(_.toInt)),
        grade =
          recordedStudent.filter(_.needsWritingToSits).flatMap(_.latestGrade)
            .orElse(member.firstAgreedGrade)
            .orElse(recordedStudent.flatMap(_.latestGrade))
            .orElse(member.firstDefinedGrade),
        needsWritingToSits = recordedStudent.exists(_.needsWritingToSits),
        outOfSync =
          recordedStudent.exists(!_.needsWritingToSits) && (
            recordedStudent.flatMap(_.latestMark).exists(m => !member.firstDefinedMark.map(_.toInt).contains(m)) ||
            recordedStudent.flatMap(_.latestGrade).exists(g => !member.firstAgreedGrade.contains(g))
          ),
        agreed = recordedStudent.exists(!_.needsWritingToSits) && member.firstAgreedMark.nonEmpty
      )
    }
  }

  @ModelAttribute("isGradeValidation")
  def isGradeValidation(@PathVariable assessmentComponent: AssessmentComponent): Boolean =
    assessmentComponent.module.adminDepartment.assignmentGradeValidation

  @RequestMapping
  def formView: String = "marks/admin/assessment-components/record"

  @PostMapping
  def save(
    @Valid @ModelAttribute("command") cmd: RecordAssessmentComponentMarksCommand.Command,
    errors: Errors,
    @PathVariable assessmentComponent: AssessmentComponent,
    @PathVariable upstreamAssessmentGroup: UpstreamAssessmentGroup,
    model: ModelMap
  )(implicit redirectAttributes: RedirectAttributes): String =
    if (errors.hasErrors) {
      model.addAttribute("flash__error", "flash.hasErrors")
      formView
    } else {
      cmd.apply()
      RedirectFlashing(Routes.marks.Admin.AssessmentComponents(assessmentComponent.module.adminDepartment, upstreamAssessmentGroup.academicYear), "flash__success" -> "flash.assessmentComponent.marksRecorded")
    }

}
