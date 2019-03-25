package uk.ac.warwick.tabula.web.filters

import java.util.Properties

import javax.annotation.Resource
import javax.servlet.FilterChain
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.web.filters.StaticContentHeadersFilter._
import uk.ac.warwick.tabula.web.views.UrlMethodModel
import uk.ac.warwick.util.web.filter.AbstractHttpFilter

import scala.concurrent.duration._
import scala.util.matching.Regex

object StaticContentHeadersFilter {
  val StaticContentExpiry: FiniteDuration = 365.days

  // This should match the extensions in webpack.config.babel in the BrotliPlugin test
  val BrotliCompressedExtensions: Map[String, String] = Map(
    "js" -> "application/json; charset=UTF-8",
    "css" -> "text/css; charset=UTF-8",
    "html" -> "text/html; charset=UTF-8",
    "svg" -> "image/svg+xml; charset=UTF-8",
    "map" -> "application/json; charset=UTF-8",
  )

  def setImmutableCacheHeaders(res: HttpServletResponse): Unit = {
    res.setHeader("Cache-Control", s"public, max-age=${StaticContentExpiry.toSeconds}, immutable")
    res.setDateHeader("Expires", DateTime.now.plus(StaticContentExpiry.toSeconds * 1000).getMillis)
    res.setHeader("Access-Control-Allow-Origin", "*") // Just in case we ever farm this off to a CDN
  }
}

class StaticContentHeadersFilter extends AbstractHttpFilter {

  @Resource(name = "staticHashes") var staticHashes: Properties = _

  val resourceWithHashPattern: Regex = "^/static/.*\\.[0-9]{10,16}\\.([^\\.]+)$".r

  override def doFilter(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain): Unit =
    resourceWithHashPattern.findFirstMatchIn(req.getRequestURI) match {
      case Some(m) =>
        // Already hashed resource
        setImmutableCacheHeaders(res)

        if (req.getHeader("Accept-Encoding").orEmpty.split(',').map(_.trim).contains("br") && BrotliCompressedExtensions.contains(m.group(1))) {
          // Forward the request onto the brotli-compressed version
          res.setHeader("Content-Encoding", "br")

          // Need to set Content-Type ourselves
          res.setHeader("Content-Type", BrotliCompressedExtensions(m.group(1)))

          req.getRequestDispatcher(s"${req.getRequestURI}.br").forward(req, res)
        } else {
          chain.doFilter(req, res)
        }

      case _ if staticHashes.containsKey(req.getRequestURI.substring("/static/".length)) =>
        // Send a redirect to the (current) immutable version of the resource
        res.sendRedirect(UrlMethodModel.addSuffix(req.getRequestURI, staticHashes))

      case _ =>
        // We don't know what to do with this, just let it carry on
        chain.doFilter(req, res)
    }

}
