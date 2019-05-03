package uk.ac.warwick.tabula.web.controllers.mitcircs

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, ViewViewableCommand}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.permissions.Permissions
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
  ): ViewViewableCommand[MitigatingCircumstancesSubmission] = {
    mustBeLinked(mandatory(submission), mandatory(student))
    new ViewViewableCommand(Permissions.MitigatingCircumstancesSubmission.Read, mandatory(submission))
  }

  @RequestMapping
  def render(@ModelAttribute("command") cmd: Appliable[MitigatingCircumstancesSubmission], @PathVariable submission: MitigatingCircumstancesSubmission): Mav = {
    Mav("mitcircs/view",
      "submission" -> cmd.apply(),
      "isSelf" -> user.universityId.maybeText.contains(submission.student.universityId)
    ).crumbs(breadcrumbsStudent(activeAcademicYear, submission.student.mostSignificantCourse, ProfileBreadcrumbs.Profile.PersonalCircumstances): _*)
  }

}
