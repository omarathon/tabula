package uk.ac.warwick.tabula.data

import org.hibernate.{Session, ScrollableResults}
import uk.ac.warwick.tabula.helpers.Closeables

/**
 * Wrapper for ScrollableResults. The Scrollable class does not have
 * any methods on it for iterating - first call take or takeWhile to
 * specify a condition for when to stop. Then you can call map or foreach
 * on the result.
 *
 * scrollable.take(100).map(..)
 * scrollable.takeWhile { a => ... }.map(..)
 *
 * You MUST call map or foreach exactly, to ensure the results are closed.
 */
class Scrollable[A](results: ScrollableResults, session: Session) {
	/**
	 * Return a result that only returns this number of results as a maximum.
	 * If there's always a maximum then it might be more efficient to set max
	 * results on the query itself. But this is useful if you want a DAO method
	 * to return a large set that can be limited later by the calling code.
	 */
	def take(count: Int): LimitedScrollable = new CountLimitedScrollable(count)
	def takeWhile(f: (A) => Boolean): LimitedScrollable = new WhileLimitedScrollable(f)

	trait LimitedScrollable extends Traversable[A] {
		def map[B](f: (A) => B): Seq[B]
		def foreach[U](f: (A) => U) = map(f)
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
				shouldContinue = when(entity)
				if (shouldContinue) {
					result = f(entity) :: result
					session.evict(entity)
				}
			}
			result
		}
	}
}
