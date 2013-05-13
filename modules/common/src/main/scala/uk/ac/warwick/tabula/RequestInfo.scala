package uk.ac.warwick.tabula

import uk.ac.warwick.util.web.Uri
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.context.request.RequestAttributes
import uk.ac.warwick.tabula.helpers.RequestLevelCache

/**
 * Stores information about the current request, such as the
 * current user.
 *
 * RequestInfo should be available even for scheduled jobs that
 * aren't directly part of an HTTP request so it should not expose
 * any Servlet specific stuff.
 *
 * It will be available anywhere from the thread but this should
 * not be used as an excuse to use it as a dumping ground for
 * "globals". Use dependency injection where possible. This is used
 * for things like the current user in situations like audit logging,
 * where it isn't appropriate to pass the user in to the method.
 */
class RequestInfo(
	val user: CurrentUser,
	val requestedUri: Uri,
	val requestParameters: Map[String, List[String]],
	val ajax: Boolean = false,
	val maintenance: Boolean = false,
	val requestLevelCache: RequestLevelCache = new RequestLevelCache)

object RequestInfo {
	private val threadLocal = new ThreadLocal[Option[RequestInfo]] {
		override def initialValue = None
	}
	def fromThread = threadLocal.get
	def open(info: RequestInfo) = threadLocal.set(Some(info))

	def use[A](info: RequestInfo)(fn: => A): A =
		try { open(info); fn }
		finally close

	def close = {
		fromThread map { _.requestLevelCache.shutdown }
		threadLocal.remove
	}
	
	def mappedPage = {
		// get the @RequestMapping (without path variables resolved), so that users don't get the same popup again
		// for a given kind of page with only variables changing
		val requestAttributes = RequestContextHolder.getRequestAttributes.asInstanceOf[ServletRequestAttributes]
		val mappedPage = requestAttributes.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST).toString
		
		mappedPage
	}
}