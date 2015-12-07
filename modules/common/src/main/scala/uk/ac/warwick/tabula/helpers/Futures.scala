package uk.ac.warwick.tabula.helpers

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

trait Futures {

	def flatten[A, M[X] <: TraversableOnce[X]](in: Seq[Future[M[A]]])(implicit executor: ExecutionContext): Future[Seq[A]] = {
		val p = scala.concurrent.Promise[Seq[A]]

		// If any of the Futures fail, fire the first failure up to the Promise
		in.foreach { _.onFailure { case t => p.tryFailure(t) } }

		// Get the sequential result of the futures and flatten them
		Future.sequence(in).foreach { results => p.trySuccess(results.flatten) }

		p.future
	}

}

object Futures extends Futures