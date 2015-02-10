package uk.ac.warwick.tabula.api.web.controllers

import javax.servlet.http.HttpServletResponse

import org.springframework.http.{HttpStatus, MediaType}
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, RequestBody}
import uk.ac.warwick.tabula.api.commands.JsonApiRequest
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.{JSONView, JSONErrorView}

abstract class ApiController extends BaseController

trait ReadApi {
	self: ApiController =>

}

trait CreateApi[A, B, C <: JsonApiRequest[A], D <: Appliable[B] with A with SelfValidating] {
	self: ApiController =>

	@RequestMapping(method = Array(POST), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
	def create(@RequestBody request: C, @ModelAttribute("createCommand") command: D, errors: Errors)(implicit response: HttpServletResponse) = {
		request.copyTo(command, errors)
		command.validate(errors)

		if (errors.hasErrors) {
			response.setStatus(HttpStatus.BAD_REQUEST.value())

			Mav(new JSONErrorView(errors, Map("success" -> false, "status" -> HttpStatus.BAD_REQUEST.value())))
		} else {
			val result = command.apply()
			Mav(new JSONView(Map("success" -> true) ++ toJson(request, result)))
		}
	}

	def toJson(request: C, result: B): Map[String, _]

}