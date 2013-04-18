package uk.ac.warwick.tabula.web.controllers

import uk.ac.warwick.spring.Wire
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping

import javax.servlet.http.HttpServletRequest
import uk.ac.warwick.tabula.system.exceptions.ExceptionResolver
import uk.ac.warwick.tabula.web.Mav

@Controller
class ErrorController extends BaseController {

	override val loggerName = "Exceptions"

	var exceptionResolver = Wire[ExceptionResolver]

	@RequestMapping(Array("/error"))
	def generalError(request: HttpServletRequest): Mav = {
		exceptionResolver.doResolve(request.getAttribute("javax.servlet.error.exception").asInstanceOf[Throwable])
			.noLayoutIf(ajax)
	}

	@RequestMapping(Array("/error/404"))
	def pageNotFound(@RequestHeader("X-Requested-Uri") requestedUri: String) = {
		Mav("errors/404", "requestedUri" -> requestedUri).noLayoutIf(ajax)
	}

}