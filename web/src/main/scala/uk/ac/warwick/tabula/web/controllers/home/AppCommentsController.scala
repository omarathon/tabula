package uk.ac.warwick.tabula.web.controllers.home


import javax.servlet.http.HttpServletRequest
import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.home.{AppCommentCommand, AppCommentCommandRequest}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, UserSettingsService}

import scala.concurrent.Future

@Controller
@RequestMapping(Array("/help"))
class AppCommentsController extends BaseController with AutowiringModuleAndDepartmentServiceComponent {

  validatesSelf[SelfValidating]

  var settingsService: UserSettingsService = Wire.auto[UserSettingsService]

  @ModelAttribute("command")
  def command = AppCommentCommand(user)

  @ModelAttribute("Recipients")
  def Recipients = AppCommentCommand.Recipients

  @RequestMapping(method = Array(GET, HEAD))
  def form(@ModelAttribute("command") cmd: Appliable[Future[JBoolean]]): Mav = {
    val maybeUser = Option(user).toSeq.filter(_.loggedIn)
    val deptAdmins = maybeUser.flatMap(u => moduleAndDepartmentService.getDepartmentByCode(u.apparentUser.getDepartmentCode).toSeq.flatMap(_.owners.users))
    val atLeastOneEmailableDeptAdmin = deptAdmins.exists(da => da.isFoundUser && da.getEmail.hasText && settingsService.getByUserId(da.getUserId).exists(_.deptAdminReceiveStudentComments))
    val homeDept = maybeUser.flatMap(u => moduleAndDepartmentService.getDepartmentByCode(u.apparentUser.getDepartmentCode).toSeq.map(d => d.name)).headOption

    Mav("home/comments",
      "hasDeptAdmin" -> deptAdmins.nonEmpty,
      "atLeastOneEmailableDeptAdmin" -> atLeastOneEmailableDeptAdmin,
      "homeDept" -> homeDept.getOrElse("unknown")
    )
  }

  @RequestMapping(method = Array(POST))
  def submit(@Valid @ModelAttribute("command") cmd: Appliable[Future[JBoolean]] with AppCommentCommandRequest, errors: Errors)(implicit request: HttpServletRequest): Mav = {
    if (errors.hasErrors) {
      form(cmd)
    } else {
      request.getHeader("user-agent").maybeText.foreach(cmd.browser = _)
      request.getRemoteAddr.maybeText.foreach(cmd.ipAddress = _)
      cmd.apply()
      Mav("home/comments-success", "previousPage" -> cmd.url)
    }

  }

}
