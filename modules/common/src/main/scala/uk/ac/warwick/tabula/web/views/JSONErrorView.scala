package uk.ac.warwick.tabula.web.views

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.JavaImports._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.springframework.validation.Errors
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import uk.ac.warwick.spring.Wire


class JSONErrorView(val errors: Errors, val additionalData: Map[String, String])  extends JSONView {

	def this( errors: Errors) = this(errors, Map())

	var messageSource = Wire.auto[MessageSource]

	override def render(model: JMap[String, _], request: HttpServletRequest, response: HttpServletResponse) = {
		response.setContentType(getContentType)
		val out = response.getWriter
		val errorList = errors.getFieldErrors
		val errorMap = Map() ++ (errorList map (error => (error.getField, getMessage(error.getCode))))


		val errorJson = Map(
			"status" -> "error",
			"result" -> errorMap
		)

		val finalJson = errorJson ++ additionalData

		objectMapper.writeValue(out, finalJson)
	}

	def getMessage(key: String, args: Object*) = messageSource.getMessage(key, args.toArray, null)

}
