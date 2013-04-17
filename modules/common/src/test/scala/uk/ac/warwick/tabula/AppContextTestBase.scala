package uk.ac.warwick.tabula
import org.junit.runner.RunWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TransactionConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import uk.ac.warwick.spring.Wire
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
import org.reflections.Reflections
import uk.ac.warwick.tabula.commands.Command
import scala.collection.JavaConverters._
import java.lang.reflect.Modifier
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.transaction.TransactionalTestExecutionListener
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import org.springframework.test.context.support.DirtiesContextTestExecutionListener
import org.junit.After
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode._
import org.junit.AfterClass

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations=Array("/WEB-INF/applicationContext.xml"))
@ActiveProfiles(Array("test"))
abstract class AppContextTestBase extends TestBase with TransactionalTesting {
	protected def allCommandsInSystem(packageBase: String) = {
		val reflections = new Reflections(packageBase)

		reflections
			.getSubTypesOf(classOf[Command[_]])
			.asScala.toList
			.filter { clz => !Modifier.isAbstract(clz.getModifiers) }
			.sortBy { _.getPackage.getName }
	}
}

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations=Array("/WEB-INF/spring-scala-glue-support-context.xml","/WEB-INF/properties-context.xml","/WEB-INF/persistence-context.xml"))
@ActiveProfiles(Array("test"))
abstract class PersistenceTestBase extends TestBase with TransactionalTesting {
	
	
	
}

trait TransactionalTesting {
	lazy val sessionFactory = Wire[SessionFactory]
	lazy val dataSource = Wire[DataSource]("dataSource")
	lazy val transactionManager = Wire[PlatformTransactionManager]
	
	def session = sessionFactory.getCurrentSession
	
	def transactional[A](f : TransactionStatus=>A) : A = {
		val template = new TransactionTemplate(transactionManager)
		template.execute(new TransactionCallback[A] {
			override def doInTransaction(status:TransactionStatus) = {
				f(status)
			}
		})
	}
}