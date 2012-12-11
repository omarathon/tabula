package uk.ac.warwick.tabula.web.views

import org.codehaus.jackson.map.ObjectMapper
import org.springframework.web.servlet.View
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._

class CSVView(var filename: String = "tabula-data.csv", var csv: Any) extends View {
	override def getContentType() = "text/csv"

	override def render(model: JMap[String, _], request: HttpServletRequest, response: HttpServletResponse) = {
		response.setContentType(getContentType)
		response.setCharacterEncoding("UTF-8")
		response.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"");
		val out = response.getWriter
		out.println(csv.toString)
	}
	
	// for testing
	def getAsString = csv.toString
}
