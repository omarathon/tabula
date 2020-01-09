package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.cm2.feedback.CheckSitsUploadCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback, StudentMember}
import uk.ac.warwick.tabula.services.{AutowiringFeedbackForSitsServiceComponent, AutowiringProfileServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

import scala.jdk.CollectionConverters._

@Controller
@RequestMapping(Array("/coursework/admin/assignments/{assignment}/feedback/{feedback}/check-sits"))
class CheckAssignmentSitsUploadController extends CourseworkController
  with AutowiringProfileServiceComponent with AutowiringFeedbackForSitsServiceComponent {

  @ModelAttribute("command")
  def command(@PathVariable assignment: Assignment, @PathVariable feedback: Feedback): CheckSitsUploadCommand.Command =
    CheckSitsUploadCommand(mandatory(assignment), mandatory(feedback))

  @RequestMapping
  def page(
    @ModelAttribute("command") cmd: CheckSitsUploadCommand.Command,
    @PathVariable feedback: Feedback,
    @PathVariable assignment: Assignment
  ): Mav = {
    val sprCodes = feedback.universityId.flatMap(uid => profileService.getMemberByUniversityId(uid)).flatMap {
      case s: StudentMember => Some(s.freshStudentCourseDetails.map(_.sprCode))
      case _ => None
    }.getOrElse(Seq())

    Mav("cm2/admin/assignments/publish/check_sits",
      "result" -> cmd.apply(),
      "feedbackForSits" -> feedbackForSitsService.getByFeedback(feedback),
      "assessmentGroupPairs" -> feedback.assessmentGroups.map(assessGroup => (assessGroup.occurrence, assessGroup.assessmentComponent.sequence)),
      "sprCodes" -> sprCodes
    ).crumbsList(Breadcrumbs.assignment(assignment))
  }

}
