package uk.ac.warwick.tabula.web.controllers.home

import java.util

import javax.servlet.http.HttpServletRequest
import net.logstash.logback.argument.StructuredArguments
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PostMapping, RequestBody, RequestMapping}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/csp-report"))
class CspReportController extends BaseController {

  val SecurityLogger: Logger = LoggerFactory.getLogger("uk.ac.warwick.SECURITY_REPORTS")

  @PostMapping(consumes = Array(MediaType.APPLICATION_JSON_VALUE, "application/csp-report"), produces = Array("application/json"))
  def consumeReport(@RequestBody json: util.Map[String, Object], request: HttpServletRequest): Mav = {
    json.put("request_headers", new util.HashMap[String, Object]() {{
      put("user-agent", request.getHeader("User-Agent").maybeText.getOrElse("-"))
    }})
    SecurityLogger.info("{}", StructuredArguments.entries(json))

    Mav(new JSONView(Map(
      "success" -> true,
      "status" -> "ok"
    )))
  }

}
