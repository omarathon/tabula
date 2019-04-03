package uk.ac.warwick.tabula.web.filters

import java.util.Properties

import javax.annotation.Resource
import javax.servlet.FilterChain
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.web.filters.StaticContentHeadersFilter._
import uk.ac.warwick.tabula.web.views.UrlMethodModel
import uk.ac.warwick.util.web.filter.AbstractHttpFilter

import scala.concurrent.duration._
import scala.util.matching.Regex

object StaticContentHeadersFilter {
  val StaticContentExpiry: FiniteDuration = 365.days

  def setImmutableCacheHeaders(res: HttpServletResponse): Unit = {
    res.setHeader("Cache-Control", s"public, max-age=${StaticContentExpiry.toSeconds}, immutable")
    res.setDateHeader("Expires", DateTime.now.plus(StaticContentExpiry.toSeconds * 1000).getMillis)
    res.setHeader("Access-Control-Allow-Origin", "*") // Just in case we ever farm this off to a CDN
  }
}

class StaticContentHeadersFilter extends AbstractHttpFilter {

  @Resource(name = "staticHashes") var staticHashes: Properties = _

  val resourceWithHashPattern: Regex = "^/static/(.*)\\.([0-9]{10,16})\\.([^\\.]+)$".r

  def isCorrectHash(path: String, hash: String): Boolean =
    hash == staticHashes.getProperty(path)

  override def doFilter(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain): Unit =
    resourceWithHashPattern.findFirstMatchIn(req.getRequestURI) match {
      case Some(m) if isCorrectHash(s"${m.group(1)}.${m.group(3)}", m.group(2)) =>
        // Already hashed resource
        setImmutableCacheHeaders(res)

        chain.doFilter(req, res)

      case Some(m) if staticHashes.containsKey(s"${m.group(1)}.${m.group(3)}") =>
        // We're pointing at an old version of the resource, send a redirect to the new one
        res.sendRedirect(UrlMethodModel.addSuffix(s"/static/${m.group(1)}.${m.group(3)}", staticHashes))

      case _ if staticHashes.containsKey(req.getRequestURI.substring("/static/".length)) =>
        // Send a redirect to the (current) immutable version of the resource
        res.sendRedirect(UrlMethodModel.addSuffix(req.getRequestURI, staticHashes))

      case _ =>
        // We don't know what to do with this, just let it carry on
        chain.doFilter(req, res)
    }

}
