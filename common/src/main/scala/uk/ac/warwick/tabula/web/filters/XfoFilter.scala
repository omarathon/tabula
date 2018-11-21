package uk.ac.warwick.tabula.web.filters

import javax.servlet.{Filter, FilterChain}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import uk.ac.warwick.util.web.filter.AbstractHttpFilter


class XfoFilter extends AbstractHttpFilter with Filter {
	def doFilter(
			request: HttpServletRequest,
			response: HttpServletResponse,
			chain: FilterChain): Unit = {

		response.addHeader("X-Frame-Options", "SAMEORIGIN")
		chain.doFilter(request, response)
	}
}
