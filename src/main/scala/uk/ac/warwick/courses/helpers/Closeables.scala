package uk.ac.warwick.courses.helpers

import java.io.Closeable

object Closeables {
	def ensureClose[T,C <: Closeable](c:C)(fn: =>T): T = try { fn } finally { c.close }
	/**
	 * Same as #ensureClose but passes the Closeable to the callback.
	 */
	def closeThis[T,C <: Closeable](c:C)(fn: (C)=>T): T = try { fn(c) } finally { c.close }
	
}