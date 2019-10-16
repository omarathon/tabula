package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.cm2.feedback._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.userlookup.User


@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}/feedback/adjustments"))
class FeedbackAdjustmentsListController extends CourseworkController {

  type FeedbackAdjustmentListCommand = Appliable[Seq[StudentInfo]]

  @ModelAttribute("listCommand")
  def listCommand(@PathVariable assignment: Assignment): FeedbackAdjustmentListCommand =
    FeedbackAdjustmentListCommand(mandatory(assignment))

  @RequestMapping
  def list(
    @PathVariable assignment: Assignment,
    @ModelAttribute("listCommand") listCommand: FeedbackAdjustmentListCommand
  ): Mav = {
    val (studentInfo, noFeedbackStudentInfo) = listCommand.apply().partition(_.feedback.isDefined)

    Mav("cm2/admin/assignments/feedback/adjustments_list",
      "studentInfo" -> studentInfo,
      "noFeedbackStudentInfo" -> noFeedbackStudentInfo,
      "assignment" -> assignment,
      "isGradeValidation" -> assignment.module.adminDepartment.assignmentGradeValidation
    ).crumbsList(Breadcrumbs.assignment(assignment))
  }
}

object FeedbackAdjustmentsController {
  // TAB-3312 Source: https://warwick.ac.uk/services/aro/dar/quality/categories/examinations/faqs/penalties
  // TAB-7564 As of 1st August 2019 penalties for PG students have changed
  // Treat any unknowns as the standard 5 marks per day
  def latePenaltyPerDay(scd: Option[StudentCourseDetails]): Int =
    scd.flatMap(_.courseType) match {
      case Some(CourseType.PGT) if Option(scd.get.sprStartAcademicYear).exists(_ < AcademicYear.starting(2019)) => 3
      case _ => 5
    }
}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}/feedback/adjustments/{student}"))
class FeedbackAdjustmentsController extends CourseworkController with AutowiringProfileServiceComponent {

  validatesSelf[SelfValidating]

  @ModelAttribute("command")
  def formCommand(@PathVariable assignment: Assignment, @PathVariable student: User, submitter: CurrentUser) =
    AssignmentFeedbackAdjustmentCommand(mandatory(assignment), student, submitter, GenerateGradesFromMarkCommand(mandatory(assignment)))

  @RequestMapping
  def showForm(
    @ModelAttribute("command") command: Appliable[Feedback] with FeedbackAdjustmentCommandState with FeedbackAdjustmentGradeValidation,
    @PathVariable assignment: Assignment,
    @PathVariable student: User
  ): Mav = {
    val submission = assignment.findSubmission(student.getUserId)
    val daysLate = submission.map(_.workingDaysLate)

    val studentCourseDetails = submission.flatMap { submission =>
      submission.universityId
        .flatMap(uid => profileService.getMemberByUniversityId(uid).map(HibernateHelpers.initialiseAndUnproxy))
        .collect { case stu: StudentMember => stu }
        .flatMap(_.mostSignificantCourseDetails)
    }

    val latePenaltyPerDay = FeedbackAdjustmentsController.latePenaltyPerDay(studentCourseDetails)
    val marksSubtracted = daysLate.map(latePenaltyPerDay * _)

    val proposedAdjustment = {
      if (assignment.openEnded) None
      else {
        for (am <- command.feedback.actualMark; ms <- marksSubtracted)
          yield Math.max(0, am - ms)
      }
    }

    Mav("cm2/admin/assignments/feedback/adjustments", Map(
      "daysLate" -> daysLate,
      "marksSubtracted" -> marksSubtracted,
      "proposedAdjustment" -> proposedAdjustment,
      "latePenalty" -> latePenaltyPerDay,
      "isGradeValidation" -> assignment.module.adminDepartment.assignmentGradeValidation,
      "gradeValidation" -> command.gradeValidation.orNull
    )).noLayout()
  }

  @RequestMapping(method = Array(POST))
  def submit(
    @Valid @ModelAttribute("command") command: Appliable[Feedback] with FeedbackAdjustmentCommandState with FeedbackAdjustmentGradeValidation,
    errors: Errors,
    @PathVariable assignment: Assignment,
    @PathVariable student: User
  ): Mav = {
    if (errors.hasErrors) {
      showForm(command, assignment, student)
    } else {
      command.apply()
      Mav("ajax_success").noLayout()
    }
  }

}
