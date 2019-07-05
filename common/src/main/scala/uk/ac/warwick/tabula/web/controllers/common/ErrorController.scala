package uk.ac.warwick.tabula.web.controllers.common

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestHeader
import uk.ac.warwick.sso.client.CSRFFilter
import uk.ac.warwick.tabula.system.exceptions.ExceptionResolver
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.helpers.HttpServletRequestUtils._
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
class ErrorController extends BaseController {

  override val loggerName = "Exceptions"

  @Autowired var exceptionResolver: ExceptionResolver = _

  @RequestMapping(Array("/error"))
  def generalError(implicit request: HttpServletRequest, response: HttpServletResponse): Mav = {
    exceptionResolver.doResolve(request.getAttribute("javax.servlet.error.exception").asInstanceOf[Throwable], Some(request), Some(response))
      .noLayoutIf(ajax)
  }

  @RequestMapping(Array("/error/404"))
  def pageNotFound(@RequestHeader(value = "X-Requested-Uri", required = false) requestedUri: String)(implicit request: HttpServletRequest, response: HttpServletResponse): Mav = {
    if (request.isJsonRequest) {
      Mav(new JSONView(Map(
        "success" -> false,
        "status" -> "not_found",
        "errors" -> Array(Map("message" -> "We don't know anything about this page"))
      )))
    } else {
      Mav("errors/404", "requestedUri" -> requestedUri).noLayoutIf(ajax)
    }
  }

  @RequestMapping(Array("/error/400"))
  def badRequest(implicit request: HttpServletRequest, response: HttpServletResponse): Mav = {
    val csrfError = request.getAttribute(CSRFFilter.CSRF_ERROR)
    if (request.isJsonRequest) {
      Mav(new JSONView(Map(
        "success" -> false,
        "status" -> "bad_request",
        "errors" -> Array(Map("message" -> (if (csrfError != null) "Missing/invalid CSRF token" else "The server cannot or will not process the request due to something that is perceived to be a client error.")))
      )))
    } else {
      if (csrfError != null) {
        Mav("errors/400_csrf", "csrfError" -> csrfError).noLayoutIf(ajax)
      } else {
        Mav("errors/400").noLayoutIf(ajax)
      }
    }
  }

}
