package uk.ac.warwick.tabula.web.controllers.common

import javax.servlet.http.HttpServletRequest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestHeader
import uk.ac.warwick.tabula.system.exceptions.ExceptionResolver
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
class ErrorController extends BaseController {

	override val loggerName = "Exceptions"

	@Autowired var exceptionResolver: ExceptionResolver = _

	@RequestMapping(Array("/error"))
	def generalError(request: HttpServletRequest): Mav = {
		exceptionResolver.doResolve(request.getAttribute("javax.servlet.error.exception").asInstanceOf[Throwable])
			.noLayoutIf(ajax)
	}

	@RequestMapping(Array("/error/404"))
	def pageNotFound(@RequestHeader(value="X-Requested-Uri", required=false) requestedUri: String) = {
		Mav("errors/404", "requestedUri" -> requestedUri).noLayoutIf(ajax)
	}

}