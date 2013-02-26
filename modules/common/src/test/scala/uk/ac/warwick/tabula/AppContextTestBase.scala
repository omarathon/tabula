package uk.ac.warwick.tabula
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
import org.reflections.Reflections
import uk.ac.warwick.tabula.commands.Command
import scala.collection.JavaConverters._
import java.lang.reflect.Modifier

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations=Array("/WEB-INF/applicationContext.xml"))
@ActiveProfiles(Array("test"))
abstract class AppContextTestBase extends TestBase with ContextSetup with TransactionalTesting {
	protected def allCommandsInSystem(packageBase: String) = {
		val reflections = new Reflections(packageBase)

		reflections
			.getSubTypesOf(classOf[Command[_]])
			.asScala.toList
			.filter { clz => !Modifier.isAbstract(clz.getModifiers) }
			.sortWith((c1, c2) => {
				val p1 = c1.getPackage.getName
				val p2 = c2.getPackage.getName
				
				if (p1 == p2) c1.getName < c2.getName
				else p1 < p2
			})
	}
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
	
	def transactional[A](f : TransactionStatus=>A) : A = {
		val template = new TransactionTemplate(transactionManager)
		template.execute(new TransactionCallback[A] {
			override def doInTransaction(status:TransactionStatus) = {
				f(status)
			}
		})
	}
}