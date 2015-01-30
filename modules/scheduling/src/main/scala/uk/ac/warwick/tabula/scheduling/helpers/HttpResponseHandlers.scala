package uk.ac.warwick.tabula.scheduling.helpers

import org.apache.http.HttpResponse
import org.apache.http.client.ResponseHandler

trait HttpResponseHandlers {
	def handle[A](fn: => (HttpResponse => A)) = new FunctionalResponseHandler[A](fn)
	
	def convert[A,B](pair: uk.ac.warwick.util.collections.Pair[A, B]) = (pair.getLeft, pair.getRight)
}

class FunctionalResponseHandler[A](fn: => (HttpResponse => A)) extends ResponseHandler[A] {
	override def handleResponse(response: HttpResponse) = fn(response)
}