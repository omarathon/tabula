package uk.ac.warwick.courses
import org.junit.runner.RunWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TransactionConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.beans.factory.annotation.Autowired
import org.hibernate.SessionFactory
import org.hibernate.Transaction
import org.springframework.transaction._
import org.springframework.transaction.support._

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations=Array("/WEB-INF/applicationContext.xml"))
@TransactionConfiguration()
@ActiveProfiles(Array("test"))
abstract class AppContextTestBase extends TestBase {
	@Autowired var sessionFactory:SessionFactory =_
	@Autowired var transactionManager:PlatformTransactionManager =_
	
	def session = sessionFactory.getCurrentSession
	
	def transactional[T](f : TransactionStatus=>T) {
		val template = new TransactionTemplate(transactionManager)
		template.execute(new TransactionCallback[T] {
			override def doInTransaction(status:TransactionStatus) = {
				f(status)
			}
		})
	}
}