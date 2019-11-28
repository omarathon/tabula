package uk.ac.warwick.tabula.helpers

import java.util.concurrent.Executors

import org.hibernate.{FlushMode, SessionFactory}
import org.springframework.orm.hibernate5.{SessionFactoryUtils, SessionHolder}
import org.springframework.transaction.support.TransactionSynchronizationManager
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.EarlyRequestInfo

import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext

/**
  * One big problem with threads is that Hibernate binds the current session to a thread local, and then other
  * things depend on it. What's a safe way to do that? Well there isn't one, really. We can open a new read-only
  * session and bind that, though, which is better than always having a null session.
  */
class SessionAwareExecutionContext(parallelism: Int) extends ExecutionContext {
  private lazy val executor = ExecutionContext.fromExecutorService(Executors.newWorkStealingPool(parallelism))

  private lazy val sessionFactory: Option[SessionFactory] = Wire.option[SessionFactory]

  override def execute(runnable: java.lang.Runnable): Unit = {
    // Bind an EarlyRequestInfo as well while we're here, for RequestLevelCaching
    val earlyRequestInfo = EarlyRequestInfo.fromThread

    sessionFactory match {
      case None =>
        // Mostly for tests. To avoid there not being a session available, we'll re-use the existing
        // one - we don't do this in the general case because session's aren't supposed to be thread-safe

        // Grab any resources bound to ThreadLocals and re-bind them inside the Runnable
        val resources = Map(TransactionSynchronizationManager.getResourceMap.asScala.toSeq: _*)

        executor.execute(Runnable {
          // Don't try and rebind or unbind something already bound
          val resourcesToBind = resources.filterNot { case (key, _) => TransactionSynchronizationManager.hasResource(key) }

          try {
            resourcesToBind.foreach { case (key, value) => TransactionSynchronizationManager.bindResource(key, value) }
            earlyRequestInfo.foreach(EarlyRequestInfo.open)
            runnable.run()
          } finally {
            earlyRequestInfo.foreach(_ => EarlyRequestInfo.close())
            resourcesToBind.keys.foreach(TransactionSynchronizationManager.unbindResource)
          }
        })

      case Some(sf) => executor.execute(Runnable {
        val session = sf.openSession()
        session.setDefaultReadOnly(true)
        session.setHibernateFlushMode(FlushMode.MANUAL)

        val sessionHolder = new SessionHolder(session)

        try {
          TransactionSynchronizationManager.bindResource(sf, sessionHolder)
          earlyRequestInfo.foreach(EarlyRequestInfo.open)
          runnable.run()
        } finally {
          earlyRequestInfo.foreach(_ => EarlyRequestInfo.close())
          TransactionSynchronizationManager.unbindResourceIfPossible(sf)
          SessionFactoryUtils.closeSession(session)
        }
      })
    }
  }

  override def reportFailure(cause: Throwable): Unit = executor.reportFailure(cause)
}
