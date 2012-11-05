package uk.ac.warwick.courses.data

import org.springframework.transaction._
import org.springframework.transaction.support._
import org.springframework.transaction.annotation._
import org.springframework.transaction.interceptor._
import org.springframework.beans.factory.annotation.{Autowired, Configurable}
import scala.annotation.target.field

@Configurable
object Transactions extends TransactionAspectSupport {

	// unused as we skip the method that calls it, but it checks that an attribute source is set.
	setTransactionAttributeSource(new MatchAlwaysTransactionAttributeSource)
	
	@Autowired var transactionManager: PlatformTransactionManager = _
	override def getTransactionManager() = transactionManager
	
	private var enabled = true

	/** Disable transaction processing inside this method block.
	  * Should be for testing only.
	  * TODO better way of disabling transactions inside tests?
	  */
	def disable(during: =>Unit) = {
		try {
			enabled = false
			during
		} finally {
			enabled = true
		}
	}

	/** Does some code in a transaction.
	 */
	def transactional[T](
			readOnly: Boolean = false, 
			propagation: Propagation = Propagation.REQUIRED
		)(f: => T): T = {

		if (enabled) {
				val attribute = new DefaultTransactionAttribute
				attribute.setReadOnly(readOnly)
				attribute.setPropagationBehavior(propagation.value())
			  handle(f, attribute)
		} else { 
			// transactions disabled, just do the work.
			f
		}
	}

	/** Similar to transactional but a bit more involved. Provides access
	  * to the TransactionStatus.
	  */
	def useTransaction[T](
			readOnly: Boolean = false,
			propagation: Propagation = Propagation.REQUIRED
		)(f: TransactionStatus => T) = {

		if (enabled) {
			val template = new TransactionTemplate(getTransactionManager())
			template.setReadOnly(readOnly)
			template.setPropagationBehavior(propagation.value())
			template.execute(new TransactionCallback[T] {
				override def doInTransaction(status: TransactionStatus) = f(status)
			})
		} else {
			f
		}

	}
  
  private def handle[T](f: => T, attribute: TransactionAttribute): T = {
	  try {
	  	createTransactionIfNecessary(getTransactionManager(), attribute, "Transactions.transactional()" )
	  	val result = f
	  	commitTransactionAfterReturning(TransactionSupport.currentTransactionInfo)
	  	result
	  } catch {
	  	case t => {
	  		completeTransactionAfterThrowing(TransactionSupport.currentTransactionInfo, t)
	  		throw t
	  	}
	  } finally {
	  	cleanupTransactionInfo(TransactionSupport.currentTransactionInfo);
	  }
	}
  
}