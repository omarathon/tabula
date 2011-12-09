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
import uk.ac.warwick.courses.data.model.Module
import scala.collection.JavaConversions._
import uk.ac.warwick.courses.data.model.Department
import javax.validation.Validation
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

class ApplicationTest extends AppContextTestBase {
    
    @Autowired var annotationMapper:RequestMappingHandlerMapping =_
    
    @Test def handlerMappings = {
    	annotationMapper.getHandlerMethods.size should not be (0)
    	for ((info,method) <- annotationMapper.getHandlerMethods()) {
    		
    	}
    }
    
    @Transactional @Test def hibernatePersistence = {
	  val assignment = new Assignment
	  assignment.name = "Cake Studies 1"
	  session.save(assignment)
	  assignment.id should not be (null)
	  
	  session.flush
	  session.clear
	  
	  val fetchedAssignment = session.get(classOf[Assignment], assignment.id).asInstanceOf[Assignment]
	  fetchedAssignment.name should be("Cake Studies 1")
	  fetchedAssignment.academicYear should be(assignment.academicYear)
	}
    
    /*
     * A post-load event in Department makes sure that a null owners
     * property is replaced with a new empty group on load.
     */
    @Transactional @Test def departmentLoadEvent {
      val dept = new Department
      dept.code = "ch"
      dept.owners = null
      session.save(dept)
      
      val id = dept.id
      
      session.flush
      session.clear
      
      session.load(classOf[Department], id) match {
        case loadedDepartment:Department => loadedDepartment.owners should not be(null)
        case _ => fail("Department not found")
      }
      
    }    
    
    @Transactional @Test def getModules = {
      val modules = session.createCriteria(classOf[Module]).list
      modules.size should be (2)
      modules(0).asInstanceOf[Module].department.name should be ("Computer Science")
    }
    
}
