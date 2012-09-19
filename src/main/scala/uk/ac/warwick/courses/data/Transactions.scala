package uk.ac.warwick.courses.data

import scala.annotation.target.field
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction._
import org.springframework.transaction.support._

trait Transactions {

	@field @Autowired var transactionManager: PlatformTransactionManager = _

	/**
	 * Does some code in a transaction. Usually you can just put @Transactional
	 * on a method instead, but sometimes that isn't sufficient.
	 */
	protected def transactional[T](f: TransactionStatus => T) {
		val template = new TransactionTemplate(transactionManager)
		template.execute(new TransactionCallback[T] {
			override def doInTransaction(status: TransactionStatus) = f(status)
		})
	}

}