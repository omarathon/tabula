package uk.ac.warwick.tabula.helpers

import java.io.Closeable
import java.sql.{Connection, PreparedStatement, ResultSet}

import org.hibernate.{ScrollableResults, Session}

import scala.util.control.NonFatal

object Closeables {
	def ensureClose[T, C <: Closeable](c: C)(fn: => T): T = withResources(c)(_ => fn)
	/**
	 * Same as #ensureClose but passes the Closeable to the callback, so you don't
	 * have to hold on to the variable yourself.
	 */
	def closeThis[T, C <: Closeable](c: C)(fn: C => T): T = withResources(c)(fn)

	def withResources[T <: AutoCloseable, V](r: => T)(f: T => V): V = {
		val resource: T = r
		require(resource != null, "resource is null")
		var exception: Throwable = null
		try {
			f(resource)
		} catch {
			case NonFatal(e) =>
				exception = e
				throw e
		} finally {
			closeAndAddSuppressed(exception, resource)
		}
	}

	private def closeAndAddSuppressed(e: Throwable,
		resource: AutoCloseable): Unit = {
		if (e != null) {
			try {
				resource.close()
			} catch {
				case NonFatal(suppressed) =>
					e.addSuppressed(suppressed)
			}
		} else {
			resource.close()
		}
	}

	def closeThis[T](s: Session)(fn: Session => T): T = try fn(s) finally s.close()
	def closeThis[T](c: Connection)(fn: Connection => T): T = try fn(c) finally c.close()
	def closeThis[T](s: PreparedStatement)(fn: PreparedStatement => T): T = try fn(s) finally s.close()
	def closeThis[T](rs: ResultSet)(fn: ResultSet => T): T = try fn(rs) finally rs.close()
	def closeThis[T](s: ScrollableResults)(fn: ScrollableResults => T): T = try fn(s) finally s.close()
}