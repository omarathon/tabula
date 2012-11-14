package uk.ac.warwick.tabula.home
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
import uk.ac.warwick.tabula.TestBase

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations=Array("/WEB-INF/applicationContext.xml"))
@ActiveProfiles(Array("test"))
abstract class AppContextTestBase extends TestBase with ContextSetup {
	
}

trait ContextSetup {
	@Autowired var beans: AbstractAutowireCapableBeanFactory =_
	
	@Before def setupCtx {
		
	}
}