package uk.ac.warwick.tabula.system

import java.util
import java.util.UUID

import freemarker.template.TemplateMethodModelEx
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import play.api.libs.json.{JsObject, Json}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.system.CspInterceptor._
import uk.ac.warwick.tabula.{AutowiringTopLevelUrlComponent, EarlyRequestInfo, Features, RequestInfo}

import scala.collection.immutable.ListMap
import scala.concurrent.duration._

object CspInterceptor {
  def generateNonce(): String = UUID.randomUUID().toString.replaceAll("[^a-z0-9]", "")
}

class CspInterceptor extends HandlerInterceptorAdapter with AutowiringTopLevelUrlComponent {
  lazy val cspReportUrl: String = s"$toplevelUrl/csp-report"
  lazy val cspReportUrlEnforced: String = cspReportUrl + "?enforced=true"

  var features: Features = Wire[Features]

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

      // AJAX request for My Warwick alert unread count and campus-cms (per CLogS reports)
      "connect-src" -> Some("'self' https://my.warwick.ac.uk https://campus-cms.warwick.ac.uk"),

      "form-action" -> Some("'self'"),

      "frame-ancestors" -> Some("'self'"),

      // see https://mathiasbynens.github.io/rel-noopener/
      "disown-opener" -> None,

      // CSPv2 report endpoint
      "report-uri" -> Some(cspReportUrl),

      // CSPv3 report group
      "report-to" -> Some("csp-reports")
    )

  def enforcingRules(nonce: String): ListMap[String, Option[String]] =
    ListMap(
      "default-src" -> Some("* data:"),

      // Allow pretty much everything for now
      // ssl.google-analytics.com for GA
      "script-src" -> Some(s"'self' 'unsafe-inline' 'unsafe-eval' https://ssl.google-analytics.com 'report-sample'"),

      // CLogS: csp-report.effective-directive:"font-src" brought up blocked data: URIs, being on the safe side here
      "font-src" -> Some("'self' data: https://fonts.googleapis.com https://fonts.gstatic.com"),

      // I found CLogS reports to suggest this hadn't been properly suppressed for submissions/download URLs
      // "object-src" -> Some("'none'"),

      // My Warwick or web sign-on account popover, or campus map
      // I checked on CLogS, the only violations I can see are from malware and Chinese browsers
      "frame-src" -> Some("'self' https://my.warwick.ac.uk https://websignon.warwick.ac.uk https://campus.warwick.ac.uk"),

      // AJAX request for My Warwick alert unread count, https://campus-cms.warwick.ac.uk via CLogS
      "connect-src" -> Some("'self' https://my.warwick.ac.uk https://campus-cms.warwick.ac.uk"),

      // No CLogS reports for violations of this directive
      "frame-ancestors" -> Some("'self'"),

      // style-src, if this isn't here it defaults to the default-src which .. despite being * .. does not allow inlines
      "style-src" -> Some("* data: 'unsafe-inline' 'report-sample'"),

      // see https://mathiasbynens.github.io/rel-noopener/
      "disown-opener" -> None,

      // CSPv2 report endpoint
      "report-uri" -> Some(cspReportUrlEnforced),

      // CSPv3 report group
      "report-to" -> Some("csp-reports-enforced"),
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
      toHeaderString(reportingRules(nonce))
    )
    if (features.enforceCsp) {
      response.setHeader(
        "Content-Security-Policy",
        toHeaderString(enforcingRules(nonce))
      )
    }
    val reportGroupLifetime = 365.25.days.toSeconds

    // "The header’s value is interpreted as a JSON-formatted
    // array of objects without the outer [ and ]"
    // Makes perfect sense.

    response.setHeader(
      "Report-To",
      Json.stringify(generateReportingGroup("csp-reports", cspReportUrl, reportGroupLifetime)) +
        ", " + Json.stringify(generateReportingGroup("csp-reports-enforced", cspReportUrlEnforced, reportGroupLifetime))
    )

    true // allow request to continue
  }

  def toHeaderString(input: ListMap[String, Option[String]]): String = {
    input.toSeq.map {
      case (k, Some(v)) => s"$k $v"
      case (k, _) => k
    }.mkString("; ")
  }

  def generateReportingGroup(groupName: String, url: String, maxAge: Long): JsObject = {
    Json.obj(
      "group" -> groupName,
      "max_age" -> maxAge,
      "endpoints" -> Json.arr(
        Json.obj(
          "url" -> url
        )
      )
    )
  }
}

class CspNonceTag extends TemplateMethodModelEx {
  override def exec(unused: util.List[_]): String =
    RequestInfo.fromThread.map(_.nonce).orNull
}
