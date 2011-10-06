package uk.ac.warwick.courses

import org.hibernate.SessionFactory
import org.junit.runner.RunWith
import org.junit.Test
import org.scalatest.junit.JUnitSuite
import org.scalatest.junit.ShouldMatchersForJUnit
import org.springframework.context.ApplicationContext
import org.springframework.mock.web.MockServletContext
import org.springframework.orm.hibernate3.HibernateTemplate
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.test.context.support.AnnotationConfigContextLoader
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.ComponentScan
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.test.context.transaction.TransactionConfiguration
import org.springframework.transaction.annotation.Transactional

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(loader=classOf[AnnotationConfigContextLoader])
@TransactionConfiguration()
@ActiveProfiles(Array("test"))
class ApplicationTest extends ShouldMatchersForJUnit {
    
    @Autowired var sessionFactory:SessionFactory = null
    
    def session = sessionFactory.getCurrentSession
    
    @Transactional @Test def hibernatePersistence = {
	  val assignment = new Assignment
	  assignment.name = "Cake Studies 1"
	  session.save(assignment)
	  assignment.id should not be (null)
	  
	  session.flush
	  session.clear
	  
	  val fetchedAssignment = session.get(classOf[Assignment], assignment.id).asInstanceOf[Assignment]
	  fetchedAssignment.name should be("Cake Studies 1")
	}
    
}

object ApplicationTest {
  @ComponentScan(Array("uk.ac.warwick.courses.config"))
  @Configuration class ContextConfiguration
}