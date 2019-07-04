package uk.ac.warwick.tabula.web.controllers.home

import java.util

import javax.servlet.http.HttpServletRequest
import net.logstash.logback.argument.StructuredArguments
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PostMapping, RequestBody, RequestMapping}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView

import scala.collection.JavaConverters._

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

  @PostMapping(consumes = Array("application/reports+json"), produces = Array("application/json"))
  def consumeReport(@RequestBody reports: util.List[util.Map[String, Object]]): Mav = {
    reports.asScala.filter { d => Option(d.get("type")).asInstanceOf[Option[String]].contains("csp") && d.containsKey("body") }.foreach { data =>
      val json = JHashMap(data.get("body").asInstanceOf[Map[String, Object]])

      if (data.containsKey("user_agent")) {
        json.put("user-agent", data.get("user_agent"))
      } else {
        json.put("user-agent", "-")
      }

      SecurityLogger.info("{}", StructuredArguments.entries(json))
    }

    Mav(new JSONView(Map(
      "success" -> true,
      "status" -> "ok"
    )))
  }

}
