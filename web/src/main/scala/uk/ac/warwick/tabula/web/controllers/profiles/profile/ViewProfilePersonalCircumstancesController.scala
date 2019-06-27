package uk.ac.warwick.tabula.web.controllers.profiles.profile

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.mitcircs.StudentHomeCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesStudent
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfileBreadcrumbs

@Controller
@RequestMapping(Array("/profiles/view/{student}/personalcircs"))
class ViewProfilePersonalCircumstancesController extends AbstractViewProfileController {

  @RequestMapping
  def render(@PathVariable student: StudentMember): Mav = {
    val command = restricted(StudentHomeCommand(mandatory(student)))
    val info = command.map(_.apply())

    Mav("mitcircs/student-home",
      "submissions" -> info.map(_.submissions),
      "submissionsWithAcuteOutcomes" -> info.map(_.submissionsWithAcuteOutcomes),
      "hasPermission" -> securityService.can(user, Permissions.MitigatingCircumstancesSubmission.Modify, MitigatingCircumstancesStudent(student)),
      "isSelf" -> user.universityId.maybeText.contains(student.universityId)
    ).crumbs(breadcrumbsStudent(activeAcademicYear, student.mostSignificantCourse, ProfileBreadcrumbs.Profile.PersonalCircumstances): _*)
  }

}
