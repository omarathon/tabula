package uk.ac.warwick.tabula
import org.junit.runner.RunWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.beans.factory.annotation.Autowired
import org.hibernate.{Session, SessionFactory}
import org.springframework.transaction._
import org.springframework.transaction.support._
import org.junit.Before
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory
import javax.sql.DataSource
import org.reflections.Reflections
import uk.ac.warwick.tabula.commands.Command
import scala.collection.JavaConverters._
import java.lang.reflect.Modifier
import org.springframework.test.annotation.DirtiesContext
import scala.language.implicitConversions



@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations=Array("/WEB-INF/applicationContext-lazyinit.xml"))
@ActiveProfiles(Array("test"))
@DirtiesContext
abstract class AppContextTestBase extends TestBase with ContextSetup with TransactionalTesting {
	
	protected def allCommandsInSystem(packageBase: String) = {
		val reflections = new Reflections(packageBase)

		reflections
			.getSubTypesOf(classOf[Command[_]])
			.asScala.toList
			.filter { clz => !Modifier.isAbstract(clz.getModifiers) }
			.sortBy { _.getPackage.getName }
	}

	// see http://stackoverflow.com/questions/1589603/scala-set-a-field-value-reflectively-from-field-name
	implicit class FieldReflector(ref: AnyRef) {
		def getV(name: String): Any = ref.getClass.getMethods.find(_.getName == name).get.invoke(ref)
		def setV(name: String, value: Any): Unit = ref.getClass.getMethods.find(_.getName == name + "_$eq").get.invoke(ref, value.asInstanceOf[AnyRef])
	}
}

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations=Array("/WEB-INF/properties-context.xml","/WEB-INF/persistence-context.xml"))
@ActiveProfiles(Array("test"))
@DirtiesContext
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
				status.setRollbackOnly()
				f(status)
			}
		})
	}

	def flushing[A](s:Session)(f: =>A):A= {
		val a = f
		s.flush
		a
	}
}