package uk.ac.warwick.courses

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
class RequestInfo(val user:CurrentUser)

object RequestInfo {
	private val threadLocal = new ThreadLocal[Option[RequestInfo]] {
		override def initialValue = None
	}
	def fromThread = threadLocal.get
	def open(info:RequestInfo) = threadLocal.set(Some(info))
	def close = threadLocal.remove
}