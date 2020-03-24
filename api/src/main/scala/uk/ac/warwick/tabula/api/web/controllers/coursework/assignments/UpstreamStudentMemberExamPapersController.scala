package uk.ac.warwick.tabula.api.web.controllers.coursework.assignments

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.api.commands.profiles.StudentExamPapersCommand
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.controllers.coursework.assignments.UpstreamMemberExamPapersController.StudentExamPapersCommand
import uk.ac.warwick.tabula.api.web.helpers.UpstreamExamPapersToJsonConverter
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.exams.grids.ModuleRegistrationAndComponents
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}

object UpstreamMemberExamPapersController {
  type StudentExamPapersCommand = Appliable[Seq[ModuleRegistrationAndComponents]]
}


@Controller
@RequestMapping(Array("/v1/student/{student}/academicyear/{academicYear}/upstreamexams"))
class UpstreamStudentMemberExamPapersController
  extends ApiController
    with UpstreamExamPapersToJsonConverter {


  @ModelAttribute("getExamPapersCommand")
  def command(@PathVariable student: StudentMember, @PathVariable academicYear: AcademicYear): StudentExamPapersCommand = {
    StudentExamPapersCommand(mandatory(student), academicYear)
  }

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def list(@ModelAttribute("getExamPapersCommand") command: StudentExamPapersCommand, errors: Errors): Mav = {
    if (errors.hasErrors) {
      Mav(new JSONErrorView(errors))
    } else {
      val info: Seq[ModuleRegistrationAndComponents] = command.apply()

      Mav(new JSONView(Map(
        "success" -> true,
        "status" -> "ok",
        "exams" -> info.map(jsonUpstreamExamPapersObject)
      )))
    }

  }
}
