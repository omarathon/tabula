package uk.ac.warwick.tabula.web.views

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.JavaImports._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.springframework.validation.Errors
import org.springframework.context.MessageSource
import uk.ac.warwick.spring.Wire

class JSONErrorView(val errors: Errors, val additionalData: Map[String, _])  extends JSONView {

	def this(errors: Errors) = this(errors, Map())

	var messageSource = Wire.auto[MessageSource]

	override def render(model: JMap[String, _], request: HttpServletRequest, response: HttpServletResponse) = {
		response.setContentType(getContentType)
		val out = response.getWriter
		val errorList = errors.getFieldErrors
		val errorMap = Map() ++ (errorList map (error => (error.getField, getMessage(error.getCode, error.getArguments: _*))))

		val globalErrors = errors.getGlobalErrors.map { error => GlobalError(error.getCode, getMessage(error.getCode, error.getArguments: _*)) }.toArray
		val fieldErrors = errors.getFieldErrors.map { error => FieldError(error.getCode, error.getField, getMessage(error.getCode, error.getArguments: _*)) }.toArray

		val errorJson = Map(
			"status" -> "error",
			"result" -> errorMap,
			"errors" -> (globalErrors ++ fieldErrors)
		)

		val finalJson = errorJson ++ additionalData

		objectMapper.writeValue(out, finalJson)
	}

	def getMessage(key: String, args: Object*) = messageSource.getMessage(key, if (args == null) Array() else args.toArray, null)

	case class GlobalError(val code: String, val message: String)
	case class FieldError(val code: String, val field: String, val message: String)

}
