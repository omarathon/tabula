package uk.ac.warwick.tabula.helpers

import javax.servlet.http.HttpServletRequest

import org.springframework.http.MediaType
import org.springframework.web.servlet.mvc.condition.{ProducesRequestCondition, ConsumesRequestCondition}
import uk.ac.warwick.util.web.Uri

import scala.collection.JavaConverters._
import scala.language.implicitConversions

import HttpServletRequestUtils._

/**
 * Scala-style HttpServletRequest utilities. Adds the methods as implicit methods
 * on HttpServletRequest.
 */
trait HttpServletRequestUtils {
	class SuperHttpServletRequest(request: HttpServletRequest) {
		def isJsonRequest = {
			if (new ConsumesRequestCondition("application/json").getMatchingCondition(request) != null) true
			else new ProducesRequestCondition("text/html", "application/json", "text/json").getMatchingCondition(request) match {
				case null => false
				case condition if condition.getExpressions.asScala.exists(_.getMediaType == MediaType.TEXT_HTML) => false
				case _ => true
			}
		}

		def isAjaxRequest = {
			val hasXHRHeader = request.getHeader(AjaxHeader) match {
				case "XMLHttpRequest" => true
				case _ => false
			}

			val hasAJAXParam = request.getParameter("ajax") match {
				case s: String => true
				case _ => false
			}

			hasXHRHeader || hasAJAXParam
		}

		def requestedUri = Uri.parse(request.getHeader(XRequestedUriHeader) match {
			case string: String => string
			case _ => request.getRequestURL.toString
		})

		def requestParameters: Map[String, Seq[String]] = {
			val params =
				for ((key, value) <- request.getParameterMap.asScala.toMap[Any, Any])
					yield (key, value) match {
						case (key: String, values: Array[String]) => (key, values.toSeq)
					}

			params.toMap
		}
	}

	implicit def HttpServletRequestToSuperHttpServletRequest(request: HttpServletRequest) = new SuperHttpServletRequest(request)
}

object HttpServletRequestUtils extends HttpServletRequestUtils {
	val AjaxHeader = "X-Requested-With"
	val XRequestedUriHeader = "X-Requested-Uri"
}
