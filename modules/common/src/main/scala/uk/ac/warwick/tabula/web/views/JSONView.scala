package uk.ac.warwick.tabula.web.views

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.web.servlet.View
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.spring.Wire

class JSONView(var json: Any) extends View {
	var objectMapper: ObjectMapper = Wire[ObjectMapper]

	override def getContentType() = "application/json"

	override def render(model: JMap[String, _], request: HttpServletRequest, response: HttpServletResponse): Unit = {
		response.setContentType(getContentType)
		val out = response.getWriter
		objectMapper.writeValue(out, json)
	}
}
