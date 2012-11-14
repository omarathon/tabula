package uk.ac.warwick.tabula.coursework
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
import org.junit.Before
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory
import org.springframework.beans.factory.support.CglibSubclassingInstantiationStrategy
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.BeanInstantiationException
import org.springframework.beans.factory.support.SimpleInstantiationStrategy
import javax.sql.DataSource

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations=Array("/WEB-INF/applicationContext.xml"))
@ActiveProfiles(Array("test"))
abstract class AppContextTestBase extends TestBase with ContextSetup with TransactionalTesting {
	
}

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations=Array("/WEB-INF/properties-context.xml","/WEB-INF/persistence-context.xml"))
@ActiveProfiles(Array("test"))
abstract class PersistenceTestBase extends TestBase with ContextSetup with TransactionalTesting {
	
	
	
}

trait ContextSetup {
	@Autowired var beans: AbstractAutowireCapableBeanFactory =_
	
	@Before def setupCtx {
		
	}
}

trait TransactionalTesting {
	@Autowired var sessionFactory:SessionFactory =_
	@Autowired var dataSource:DataSource =_
	@Autowired var transactionManager:PlatformTransactionManager =_
	
	def session = sessionFactory.getCurrentSession
	
	def transactional[T](f : TransactionStatus=>T) : T = {
		val template = new TransactionTemplate(transactionManager)
		template.execute(new TransactionCallback[T] {
			override def doInTransaction(status:TransactionStatus) = {
				f(status)
			}
		})
	}
}