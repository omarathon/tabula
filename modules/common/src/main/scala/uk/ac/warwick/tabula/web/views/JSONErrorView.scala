package uk.ac.warwick.tabula.web.views

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.JavaImports._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.springframework.validation.Errors

import org.springframework.context.MessageSource
import uk.ac.warwick.spring.Wire


class JSONErrorView(var errors: Errors)  extends JSONView {

	var messageSource = Wire[MessageSource]

	override def render(model: JMap[String, _], request: HttpServletRequest, response: HttpServletResponse) = {
		response.setContentType(getContentType)
		val out = response.getWriter
		val errorList = errors.getFieldErrors
		val errorMap = Map() ++ (errorList map (error => (error.getField, getMessage(error.getCode))))
		val errorJson = Map("status" -> "error", "result" -> errorMap)

		objectMapper.writeValue(out, errorJson)
	}

	def getMessage(key: String, args: Object*) = messageSource.getMessage(key, args.toArray, null)

}
