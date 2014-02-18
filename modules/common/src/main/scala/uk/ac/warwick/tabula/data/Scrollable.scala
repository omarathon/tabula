package uk.ac.warwick.tabula.data

import org.hibernate.{Session, ScrollableResults}
import uk.ac.warwick.tabula.helpers.Closeables

/**
 * Wrapper for ScrollableResults. The Scrollable class does not have
 * any methods on it for iterating - first call take or takeWhile to
 * specify a condition for when to stop. Then you can call map or foreach
 * on the result.
 *
 * new Scrollable(sr).take(100).map(..)
 * new Scrollable(sr).takeWhile { a => ... }.map(..)
 *
 * You MUST call map or foreach once, to ensure the results are closed.
 */
class Scrollable[A](results: ScrollableResults, session: Session) {
	def take(count: Int): LimitedScrollable = new CountLimitedScrollable(count)
	def takeWhile(f: (A) => Boolean): LimitedScrollable = new WhileLimitedScrollable(f)

	trait LimitedScrollable {
		def map[B](f: (A) => B): Seq[B]
		def foreach(f: (A) => Unit) = map(f)
		def toSeq = map(e => e)
	}

	class CountLimitedScrollable(count: Int) extends LimitedScrollable {
		override def map[B](f: (A) => B) = Closeables.closeThis(results) { r =>
			var i = 0
			var result = List[B]()
			while (i < count && results.next()) {
				val entity: A = results.get(0).asInstanceOf[A]
				result = f(entity) :: result
				session.evict(entity)
				i += 1
			}
			result
		}
	}

	class WhileLimitedScrollable(when: (A) => Boolean) extends LimitedScrollable {
		def map[B](f: (A) => B): Seq[B] = Closeables.closeThis(results) { r =>
			var shouldContinue = true
			var result = List[B]()
			while (shouldContinue && results.next()) {
				val entity: A = results.get(0).asInstanceOf[A]
				result = f(entity) :: result
				session.evict(entity)
				shouldContinue = when(entity)
			}
			result
		}
	}
}
