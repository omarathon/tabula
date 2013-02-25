package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.util.concurrency.promise.MutablePromise
import uk.ac.warwick.util.concurrency.promise.Promise
import uk.ac.warwick.util.concurrency.promise.UnfulfilledPromiseException

trait Promises {
	
	def promise[A] = new MutablePromise[A]
	
	def promise[A](fn: => A): Promise[A] = new FunctionalPromise(fn)
	
	def optionPromise[A](fn: => Option[A]): Promise[A] = new OptionalPromise(fn)
	
}

private class FunctionalPromise[A](fn: => A) extends Promise[A] {
	
	def fulfilPromise = fn
	
}

private class OptionalPromise[A](fn: => Option[A]) extends Promise[A] {
	
	def fulfilPromise = fn match {
		case Some(value) => value
		case _ => throw new UnfulfilledPromiseException("Fulfilled promise on Option(None)")
	}
	
}
