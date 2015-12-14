package uk.ac.warwick.tabula.helpers

import org.hibernate.{FlushMode, SessionFactory}
import org.springframework.orm.hibernate4.{SessionFactoryUtils, SessionHolder}
import org.springframework.transaction.support.TransactionSynchronizationManager
import uk.ac.warwick.spring.Wire

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

trait Futures {

	def flatten[A, M[X] <: TraversableOnce[X]](in: Future[M[A]]*)(implicit executor: ExecutionContext): Future[Seq[A]] = {
		val p = scala.concurrent.Promise[Seq[A]]

		// If any of the Futures fail, fire the first failure up to the Promise
		in.foreach { _.onFailure { case t => p.tryFailure(t) } }

		// Get the sequential result of the futures and flatten them
		Future.sequence(in).foreach { results => p.trySuccess(results.flatten) }

		p.future
	}

	/**
		* Constructs a Future that contains the passed result after a duration. Useful for the pattern where you
		* have a Future with a result that you want to try for, but timeout after a period of time, you can then use
		* Future.firstCompletedOf to get either the timeout as a default or timeout value.
		*/
	def timeout[A](result: => A, duration: Duration)(implicit executor: ExecutionContext): Future[A] = Future {
		Thread.sleep(duration.toMillis)
		result
	}

}

object Futures extends Futures {

	/**
		* One big problem with threads is that Hibernate binds the current session to a thread local, and then other
		* things depend on it. What's a safe way to do that? Well there isn't one, really. We can open a new read-only
		* session and bind that, though, which is better than always having a null session.
		*/
	implicit lazy val executionContext = new ExecutionContext {
		private lazy val sessionFactory: Option[SessionFactory] = Wire.option[SessionFactory]

		override def execute(runnable: java.lang.Runnable): Unit = sessionFactory match {
			case None =>
				// Mostly for tests. To avoid there not being a session available, we'll re-use the existing
				// one - we don't do this in the general case because session's aren't supposed to be thread-safe

				// Grab any resources bound to ThreadLocals and re-bind them inside the Runnable
				val resources = TransactionSynchronizationManager.getResourceMap.asScala

				ExecutionContext.Implicits.global.execute(Runnable {
					try {
						resources.foreach { case (key, value) => TransactionSynchronizationManager.bindResource(key, value) }
						runnable.run()
					} finally {
						resources.keys.foreach(TransactionSynchronizationManager.unbindResource)
					}
				})

			case Some(sf) => ExecutionContext.Implicits.global.execute(Runnable {
				val session = sf.openSession()
				session.setDefaultReadOnly(true)
				session.setFlushMode(FlushMode.MANUAL)

				val sessionHolder = new SessionHolder(session)

				try {
					TransactionSynchronizationManager.bindResource(sf, sessionHolder)
					runnable.run()
				} finally {
					TransactionSynchronizationManager.unbindResourceIfPossible(sf)
					SessionFactoryUtils.closeSession(session)
				}
			})
		}
		override def reportFailure(cause: Throwable): Unit = ExecutionContext.Implicits.global.reportFailure(cause)
	}

}