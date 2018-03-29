package uk.ac.warwick.tabula.helpers

import java.util.concurrent.{ScheduledExecutorService, TimeoutException}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

trait Futures {

	// TODO express this using combine()
	def flatten[A, M[X] <: TraversableOnce[X]](in: Future[M[A]]*)(implicit executor: ExecutionContext): Future[Seq[A]] = {
		val p = scala.concurrent.Promise[Seq[A]]

		// If any of the Futures fail, fire the first failure up to the Promise
		in.foreach { _.onFailure { case t => p.tryFailure(t) } }

		// Get the sequential result of the futures and flatten them
		Future.sequence(in).foreach { results => p.trySuccess(results.flatten) }

		p.future
	}

	def combine[A](in: Seq[Future[A]], fn: (Seq[A] => A))(implicit executor: ExecutionContext): Future[A] = {
		val p = scala.concurrent.Promise[A]

		// If any of the Futures fail, fire the first failure up to the Promise
		in.foreach { _.onFailure { case t => p.tryFailure(t) } }

		// Get the sequential result of the futures and flatten them
		Future.sequence(in).foreach { results => p.trySuccess(fn(results)) }

		p.future
	}

	/**
		* Constructs a Future that will return the result (or failure) of the passed Future if it completes within
		* the specified Duration, else will return a failure with a TimeoutException.
		*/
	def withTimeout[A](f: Future[A], duration: Duration)(implicit executor: ExecutionContext, scheduler: ScheduledExecutorService): Future[A] = {
		val p = scala.concurrent.Promise[A]

		// After the specified time, fail the promise
		scheduler.schedule(Runnable {
			p.tryFailure(new TimeoutException)
		}, duration.length, duration.unit)

		p.tryCompleteWith(f)

		p.future
	}

	/**
		* Returns a Future that recovers from a timeout by returning an empty Option, or a non-empty option if it completes
		* successfully within the specified timeout.
		*/
	def optionalTimeout[A](f: Future[A], duration: Duration)(implicit executor: ExecutionContext, scheduler: ScheduledExecutorService): Future[Option[A]] =
		withTimeout(f, duration).map { Some(_) }
			  .recover { case _: TimeoutException => None }

}

object Futures extends Futures {
}

object ExecutionContexts {
	implicit lazy val global: ExecutionContext = new SessionAwareExecutionContext(100)

	implicit lazy val email: ExecutionContext = new SessionAwareExecutionContext(50)

	implicit lazy val timetable: ExecutionContext = new SessionAwareExecutionContext(200)
}