package uk.ac.warwick.tabula.web.controllers.mitcircs

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.mitcircs.{HomeInformation, StudentHomeCommand}
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(value = Array("/mitcircs/profile"))
class StudentHomeRedirectController extends BaseController {

  @RequestMapping
  def render: Mav = user.profile match {
    case Some(student: StudentMember) => Redirect(Routes.Student.home(student))
    case _ => throw new ItemNotFoundException()
  }

}

@Controller
@RequestMapping(value = Array("/mitcircs/profile/{student}"))
class StudentHomeController extends BaseController {

  @ModelAttribute("command")
  def command(@PathVariable student: StudentMember): Appliable[HomeInformation] = {
    StudentHomeCommand(mandatory(student))
  }

  @RequestMapping
  def render(@ModelAttribute("command") cmd: Appliable[HomeInformation], @PathVariable student: StudentMember): Mav = {
    val info = cmd.apply()
    Mav("mitcircs/student-home", "submissions" -> info.submissions)
  }

}
