package uk.ac.warwick.tabula.system

import java.util
import java.util.UUID

import freemarker.template.TemplateMethodModelEx
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import play.api.libs.json.Json
import uk.ac.warwick.tabula.system.CspInterceptor._
import uk.ac.warwick.tabula.{AutowiringTopLevelUrlComponent, EarlyRequestInfo, RequestInfo}

import scala.collection.immutable.ListMap
import scala.concurrent.duration._

object CspInterceptor {
  def generateNonce(): String = UUID.randomUUID().toString.replaceAll("[^a-z0-9]", "")
}

class CspInterceptor extends HandlerInterceptorAdapter with AutowiringTopLevelUrlComponent {
  lazy val cspReportUrl: String = s"$toplevelUrl/csp-report"

  def reportingRules(nonce: String): ListMap[String, Option[String]] =
    ListMap(
      "default-src" -> Some("'self'"),

      // photos.warwick is for the account popover; actual photos are proxied so 'self' is fine
      // data: is for Modernizr doing webp checks
      // ssl.google-analytics.com for GA
      "img-src" -> Some("'self' data: https://photos.warwick.ac.uk https://ssl.google-analytics.com"),

      // The unsafe-inline directive here is IGNORED in favour of nonces in modern browsers
      // ssl.google-analytics.com for GA
      "script-src" -> Some(s"'self' 'unsafe-inline' 'nonce-$nonce' https://ssl.google-analytics.com 'report-sample'"),

      // TODO Replace unsafe-inline with a nonce - this will mean that all style="" attributes will need replacing
      "style-src" -> Some(s"'self' 'unsafe-inline' https://fonts.googleapis.com 'report-sample'"),

      "font-src" -> Some("'self' https://fonts.googleapis.com https://fonts.gstatic.com"),

      // No Flash or other plugins - when we serve files inline this is overridden
      "object-src" -> Some("'none'"),

      // My Warwick or web sign-on account popover, or campus map
      "frame-src" -> Some("'self' https://my.warwick.ac.uk https://websignon.warwick.ac.uk https://campus.warwick.ac.uk"),

      // AJAX request for My Warwick alert unread count
      "connect-src" -> Some("'self' https://my.warwick.ac.uk"),

      "form-action" -> Some("'self'"),

      "frame-ancestors" -> Some("'self'"),

      // see https://mathiasbynens.github.io/rel-noopener/
      "disown-opener" -> None,

      // CSPv2 report endpoint
      "report-uri" -> Some(cspReportUrl),

      // CSPv3 report group
      "report-to" -> Some("csp-reports")
    )

  override def preHandle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any
  ): Boolean = {
    // Generate a fresh nonce if needed
    val nonce = EarlyRequestInfo.fromThread.map(_.nonce).getOrElse(generateNonce())

    response.setHeader(
      "Content-Security-Policy-Report-Only",
      reportingRules(nonce).toSeq.map {
        case (k, Some(v)) => s"$k $v"
        case (k, _) => k
      }.mkString("; ")
    )
    response.setHeader(
      "Report-To",
      Json.stringify(Json.obj(
        "group" -> "csp-reports",
        "max_age" -> 365.25.days.toSeconds,
        "endpoints" -> Json.arr(
          Json.obj(
            "url" -> cspReportUrl
          )
        )
      ))
    )

    true // allow request to continue
  }
}

class CspNonceTag extends TemplateMethodModelEx {
  override def exec(unused: util.List[_]): String =
    RequestInfo.fromThread.map(_.nonce).orNull
}