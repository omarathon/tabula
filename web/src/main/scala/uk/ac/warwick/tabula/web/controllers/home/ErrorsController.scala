package uk.ac.warwick.tabula.web.controllers.home

import java.util

import com.fasterxml.jackson.annotation.{JsonAutoDetect, JsonIgnoreProperties}
import javax.servlet.http.HttpServletRequest
import net.logstash.logback.argument.{StructuredArgument, StructuredArguments}
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestBody, RequestMapping, RequestMethod}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._

@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
class JsErrorObject extends Serializable {
  @BeanProperty var message: String = _
  @BeanProperty var source: String = _
  @BeanProperty var line: Int = _
  @BeanProperty var column: Int = _
  @BeanProperty var pageUrl: String = _
  @BeanProperty var stack: String = _
  @BeanProperty var user: String = _
}

@Controller
@RequestMapping(
  path = Array("/errors"),
  consumes = Array(MediaType.APPLICATION_JSON_VALUE),
  produces = Array(MediaType.APPLICATION_JSON_VALUE),
)
class ErrorsController extends BaseController with Logging {

  @RequestMapping(path = Array("/js"), method = Array(RequestMethod.POST))
  def js(@RequestBody jsErrorObjects: JList[JsErrorObject], user: CurrentUser)(implicit request: HttpServletRequest): Mav = {
    jsErrorObjects.asScala.foreach { err =>
      val message: String = err.message.maybeText.getOrElse("empty error message.")
      val args: Map[String, Any] = Seq(
        Option(err.column).map("column" -> _),
        Option(err.line).map("line" -> _),
        err.source.maybeText.map("source" -> _),
        err.pageUrl.maybeText.map("page_url" -> _),
        err.stack.maybeText.map("stack_trace" -> _),
        request.getRemoteAddr.maybeText.map("source_ip" -> _),
        request.getHeader("User-Agent").maybeText.map(ua => "request_headers" -> JMap("user-agent"-> ua)),
        if (user.apparentUser.isFoundUser) Option("username" -> user.apparentId) else None
      ).flatten.toMap

      val entries: StructuredArgument = StructuredArguments.entries(Logging.convertForStructuredArguments(args).asInstanceOf[util.Map[String, Object]])
      logger.info(message, entries)
    }
    Mav(new JSONView(Nil))
  }

}
