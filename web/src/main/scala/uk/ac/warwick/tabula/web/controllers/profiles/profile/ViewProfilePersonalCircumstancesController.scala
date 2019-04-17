package uk.ac.warwick.tabula.web.controllers.profiles.profile

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.mitcircs.StudentHomeCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfileBreadcrumbs


@Controller
@RequestMapping(Array("/profiles/view/{student}/personalcircs"))
class ViewProfilePersonalCircumstancesController extends AbstractViewProfileController {


  @RequestMapping
  def render(@PathVariable student: StudentMember): Mav = {
    val command = restricted(StudentHomeCommand(mandatory(student)))
    Mav("mitcircs/student-home",
      "submissions" -> command.map(_.apply().submissions).orNull,
      "hasPermission" -> command.isDefined,
      "isSelf" -> user.universityId.maybeText.contains(student.universityId)
    ).crumbs(breadcrumbsStudent(activeAcademicYear, student.mostSignificantCourse, ProfileBreadcrumbs.Profile.PersonalCircumstances): _*)
  }

}
