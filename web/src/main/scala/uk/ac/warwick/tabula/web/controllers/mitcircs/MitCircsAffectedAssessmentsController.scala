package uk.ac.warwick.tabula.web.controllers.mitcircs

import java.util

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import org.springframework.web.servlet.View
import play.api.libs.json.{Json, Writes}
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.mitcircs.submission.{UpstreamAffectedAssessment, MitCircsAffectedAssessmentsCommand}
import uk.ac.warwick.tabula.commands.mitcircs.submission.MitCircsAffectedAssessmentsCommand._
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONErrorView

@Controller
@RequestMapping(Array("/mitcircs/profile/{student}/affected-assessments"))
class MitCircsAffectedAssessmentsController extends BaseController {

  validatesSelf[SelfValidating]

  @ModelAttribute("command") def command(@PathVariable student: StudentMember): Command =
    MitCircsAffectedAssessmentsCommand(mandatory(student))

  // TODO change to @PostMapping once testing is complete
  @RequestMapping
  def affectedAssessments(@Valid @ModelAttribute("command") command: Command, errors: Errors): Mav =
    if (errors.hasErrors) Mav(new JSONErrorView(errors))
    else {
      Mav(new View {
        override def render(model: util.Map[String, _], request: HttpServletRequest, response: HttpServletResponse): Unit = {
          response.setContentType(getContentType)
          val out = response.getWriter
          out.write(Json.stringify(Json.toJson(command.apply())(Writes.seq(UpstreamAffectedAssessment.writesUpstreamAffectedAssessment))))
        }
        override def getContentType: String = "application/json"
      })
    }

}
