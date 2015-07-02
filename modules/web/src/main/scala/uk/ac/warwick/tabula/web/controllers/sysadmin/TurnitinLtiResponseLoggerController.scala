package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.apache.commons.io.IOUtils
import uk.ac.warwick.tabula.helpers.Logging

// TODO make this useful and not just log stuff
@Controller
@RequestMapping(value = Array("/api/turnitin-response"))
class TurnitinLtiResponseLoggerController extends BaseSysadminController with Logging {

	def inspectResponse()(implicit request: HttpServletRequest, response: HttpServletResponse) {

		debug("request body: " + IOUtils.toString(request.getInputStream))
	}

}
