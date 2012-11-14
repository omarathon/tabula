package uk.ac.warwick.tabula.coursework.web.views

import org.codehaus.jackson.map.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.web.servlet.View
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.spring.Wire

class JSONView(var json: Any) extends View {
	var objectMapper = Wire.auto[ObjectMapper]

	override def getContentType() = "application/json"

	override def render(model: JMap[String, _], request: HttpServletRequest, response: HttpServletResponse) = {
		response.setContentType(getContentType)
		val out = response.getWriter
		objectMapper.writeValue(out, json)
	}
}
