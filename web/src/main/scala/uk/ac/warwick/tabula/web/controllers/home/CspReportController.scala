package uk.ac.warwick.tabula.web.controllers.home

import java.util

import javax.servlet.http.HttpServletRequest
import net.logstash.logback.argument.StructuredArguments
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PostMapping, RequestBody, RequestMapping}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/csp-report"))
class CspReportController extends BaseController {

  val SecurityLogger: Logger = LoggerFactory.getLogger("uk.ac.warwick.SECURITY_REPORTS")

  @PostMapping(consumes = Array(MediaType.APPLICATION_JSON_VALUE, "application/csp-report"), produces = Array("application/json"))
  def consumeReport(@RequestBody report: Map[String, Any], request: HttpServletRequest): Mav = {
    val payload = report("csp-report").asInstanceOf[Map[String, Object]]
    val data = Map(
      "csp-report" -> (payload + ("mode" -> getCspMode(request))),
      "request_headers" -> Map(
        "user-agent" -> request.getHeader("User-Agent").maybeText
      )
    )

    SecurityLogger.info("{}", StructuredArguments.entries(Logging.convertForStructuredArguments(data).asInstanceOf[util.Map[String, Object]]))

    Mav(new JSONView(Map(
      "success" -> true,
      "status" -> "ok"
    )))
  }

  private def getCspMode(request: HttpServletRequest): String = {
    if (request.getQueryString.contains("enforced")) "enforced" else "reported"
  }

  @PostMapping(consumes = Array("application/reports+json"), produces = Array("application/json"))
  def consumeReport(@RequestBody reports: Seq[Map[String, Any]], request: HttpServletRequest): Mav = {
    reports.filter { r => r.get("type").contains("csp") && r.contains("body") }.foreach { report => {
      val payload = report("body").asInstanceOf[Map[String, Object]]
      val data = Map(
        "csp-report" -> (payload + ("mode" -> getCspMode(request))),
        "request_headers" -> Map(
          "user-agent" -> report.get("user_agent")
        )
      )

      SecurityLogger.info("{}", StructuredArguments.entries(Logging.convertForStructuredArguments(data).asInstanceOf[util.Map[String, Object]]))
    }
    }

    Mav(new JSONView(Map(
      "success" -> true,
      "status" -> "ok"
    )))
  }

}
