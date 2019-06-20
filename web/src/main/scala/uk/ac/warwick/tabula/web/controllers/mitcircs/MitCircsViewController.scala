package uk.ac.warwick.tabula.web.controllers.mitcircs

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.mitcircs.StudentViewMitCircsSubmissionCommand
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfileBreadcrumbs
import uk.ac.warwick.tabula.web.controllers.profiles.profile.AbstractViewProfileController

@Controller
@RequestMapping(Array("/profiles/view/{student}/personalcircs/mitcircs/view/{submission}"))
class MitCircsViewController extends AbstractViewProfileController {

  @ModelAttribute("command")
  def getCommand(
    @PathVariable student: StudentMember,
    @PathVariable submission: MitigatingCircumstancesSubmission
  ): StudentViewMitCircsSubmissionCommand.Command = {
    mustBeLinked(submission, student)
    StudentViewMitCircsSubmissionCommand(submission)
  }

  @RequestMapping
  def render(@ModelAttribute("command") cmd: StudentViewMitCircsSubmissionCommand.Command, @PathVariable submission: MitigatingCircumstancesSubmission): Mav = {
    Mav("mitcircs/view",
      "submission" -> cmd.apply(),
      "isSelf" -> user.universityId.maybeText.contains(submission.student.universityId)
    ).crumbs(breadcrumbsStudent(activeAcademicYear, submission.student.mostSignificantCourse, ProfileBreadcrumbs.Profile.PersonalCircumstances): _*)
  }

}
