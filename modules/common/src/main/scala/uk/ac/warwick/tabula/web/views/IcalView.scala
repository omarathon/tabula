package uk.ac.warwick.tabula.web.views

import org.springframework.web.servlet.View
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.JavaImports._
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.data.CalendarOutputter

class IcalView(var ical: Calendar) extends View {

	override def getContentType = "text/calendar"

	override def render(model: JMap[String, _], request: HttpServletRequest, response: HttpServletResponse) = {
		response.setContentType(getContentType)
		new CalendarOutputter().output(ical, response.getWriter)
	}
}
