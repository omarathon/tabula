package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import uk.ac.warwick.tabula.helpers.HttpServletRequestUtils._
import org.apache.commons.io.IOUtils
import uk.ac.warwick.tabula.helpers.Logging

@Controller
@RequestMapping(value = Array("/api/turnitin-response"))
class TurnitinTesterResponseController extends BaseSysadminController with Logging {

	@annotation.RequestMapping(method=Array(POST))
	def inspectResponse()(implicit request: HttpServletRequest, response: HttpServletResponse) {

		logger.info("request body: " + IOUtils.toString(request.getInputStream))
		logger.info("isJsonRequest: " + request.isJsonRequest)
	}

}
