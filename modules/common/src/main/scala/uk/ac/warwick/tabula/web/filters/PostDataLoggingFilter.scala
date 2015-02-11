package uk.ac.warwick.tabula.web.filters

import java.util

import org.apache.commons.fileupload.servlet.ServletFileUpload
import uk.ac.warwick.tabula.RequestInfo

import scala.collection.JavaConverters._

import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.apache.log4j.Logger

import uk.ac.warwick.util.web.filter.AbstractHttpFilter

class PostDataLoggingFilter extends AbstractHttpFilter with Filter {
	@transient lazy val logger = Logger.getLogger(getClass.getName)
	@transient lazy val postLogger = Logger.getLogger("POST_LOGGER")

	def doFilter(
			request: HttpServletRequest,
			response: HttpServletResponse,
			chain: FilterChain): Unit = {

		// Only run on POST requests.
		// Also only run if not multipart - partly because getParameter doesn't support it,
		// but reading parameters also messes with file uploads.
		if (request.getMethod.equalsIgnoreCase("post") && !ServletFileUpload.isMultipartContent(request)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Logging POST data for request to " + request.getRequestURI())
			}
			postLogger.info(generateLogLine(request))
		}
		
		// continue the chain
		chain.doFilter(request, response)
	}

	def generateLogLine(request: HttpServletRequest): String = {
		val data = new StringBuilder()

		data.append(RequestInfo.fromThread.map(_.user.realId).getOrElse(""))
		data.append(" ")
		data.append(request.getRequestURI)
		data.append(" ")

		val paramKeys = request.getParameterMap.keySet.asInstanceOf[util.Set[String]].asScala
		val allParams = paramKeys.flatMap { (key) =>
			request.getParameterValues(key).map { (value) =>
				key + "=" + value
			}
		}.mkString("&")

		data.append(allParams)

		data.toString
	}
}
