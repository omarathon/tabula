package uk.ac.warwick.tabula.web.controllers.mitcircs

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.tabula.data.model.StudentMember

import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(value = Array("/mitcircs/profile"))
class StudentHomeController extends BaseController {

  @RequestMapping
  def render: Mav = user.profile match {
    case Some(student: StudentMember) =>
      Redirect(Routes.Student.home(student))
    case _ => throw new ItemNotFoundException()
  }

  @RequestMapping(value = Array("{student}"))
  def render(@PathVariable student: StudentMember): Mav = {
    Mav("mitcircs/student-home")
  }

}
