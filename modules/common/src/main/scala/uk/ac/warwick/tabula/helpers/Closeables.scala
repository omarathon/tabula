package uk.ac.warwick.tabula.helpers

import java.io.Closeable

object Closeables {
	def ensureClose[T, C <: Closeable](c: C)(fn: => T): T = try fn finally c.close
	/**
	 * Same as #ensureClose but passes the Closeable to the callback, so you don't
	 * have to hold on to the variable yourself.
	 */
	def closeThis[T, C <: Closeable](c: C)(fn: (C) => T): T = try fn(c) finally c.close

}