package uk.ac.warwick.tabula.scheduling.helpers

import org.apache.http.HttpResponse
import org.apache.http.client.ResponseHandler

trait HttpResponseHandlers {
	def handle[T](fn: => (HttpResponse => T)) = new FunctionalResponseHandler[T](fn)
	
	def convert[A,B](pair: uk.ac.warwick.util.collections.Pair[A, B]) = (pair.getLeft, pair.getRight)
}

class FunctionalResponseHandler[T](fn: => (HttpResponse => T)) extends ResponseHandler[T] {
	override def handleResponse(response: HttpResponse) = fn(response)
}