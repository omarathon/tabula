package uk.ac.warwick.tabula.helpers

/**
	* Allows chaining of Ordering to enable things like: sort by first name ascending then by last name descending.
	* Mainly useful when the number of fields to sort by is decided at run time.
	*/
class ComposableOrdering[T](thisOrdering: Ordering[T]) extends Ordering[T] {

	var nextOrdering: ComposableOrdering[T] = _
	var previousOrdering: ComposableOrdering[T] = _

	def andThen(nextOrdering: Ordering[T]): ComposableOrdering[T] = {
		this.nextOrdering = new ComposableOrdering(nextOrdering)
		this.nextOrdering.previousOrdering = this
		this.nextOrdering
	}

	def doCompare(x: T, y: T): Int = {
		thisOrdering.compare(x, y) match {
			case 0 =>	Option(nextOrdering) match {
				case Some(next) => next.doCompare(x, y)
				case _ => 0
			}
			case nonZero => nonZero
		}
	}

	override def compare(x: T, y: T): Int = {
		Option(previousOrdering) match {
			case Some(previous) => previous.compare(x, y)
			case _ => doCompare(x, y)
		}
	}

}
