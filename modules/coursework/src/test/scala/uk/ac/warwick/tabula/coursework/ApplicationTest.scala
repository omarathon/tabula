package uk.ac.warwick.tabula.coursework

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
import uk.ac.warwick.tabula.data.model.Assignment
import org.springframework.test.context.support.AnnotationConfigContextLoader
import org.springframework.context.annotation.Configuration
import uk.ac.warwick.spring.Wire
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.ComponentScan
import uk.ac.warwick.tabula.data.model.Assignment
import org.springframework.test.context.transaction.TransactionConfiguration

import uk.ac.warwick.tabula.data.model.Module
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.Department
import javax.validation.Validation
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import uk.ac.warwick.tabula._
import org.junit.Ignore

class ApplicationTest extends AppContextTestBase {
    
    lazy val annotationMapper = Wire[RequestMappingHandlerMapping]
    
    var auditIndexDir = Wire[String]("${filesystem.index.audit.dir}")
    
    @Test def handlerMappings = {
    	annotationMapper.getHandlerMethods.size should not be (0)
    	for ((info,method) <- annotationMapper.getHandlerMethods()) {
    		
    	}
    }
    
    /** 
     * Check that a property in default.properties can reference
     * a property found in tabula.properties, even though the latter
     * is loaded after the former.
     * 
     * This is important for allowing "base.data.dir" to be set in
     * tabula.properties, and default.properties using that as
     * the root directory for many other directory locations.
     */
    @Test def defaultProperties = {
    	auditIndexDir should be ("target/test-tmp/index/audit")
    }
    
    @Test def hibernatePersistence = transactional { tx =>
	  val assignment = new Assignment
	  assignment.name = "Cake Studies 1"
	  assignment.academicYear = new AcademicYear(2009)
	  session.save(assignment)
	  assignment.id should not be (null)
	  
	  session.flush
	  session.clear
	  
	  val fetchedAssignment = session.get(classOf[Assignment], assignment.id).asInstanceOf[Assignment]
	  fetchedAssignment.name should be("Cake Studies 1")
	  fetchedAssignment.academicYear should be(assignment.academicYear)
	}
    
    /*
     * A post-load event in Department makes sure that a null settings
     * property is replaced with a new empty group on load.
     */
    @Test def departmentLoadEvent = transactional { tx =>
      val dept = new Department
      dept.code = "ch"
      dept.settings = null
      session.save(dept)
      
      val id = dept.id
      
      session.flush
      session.clear
      
      session.load(classOf[Department], id) match {
        case loadedDepartment:Department => loadedDepartment.settings should not be(null)
        case _ => fail("Department not found")
      }
      
    }    
    
    @Test def getModules = transactional { tx =>
      val modules = session.createCriteria(classOf[Module]).list
      modules.size should be (3)
      modules(0).asInstanceOf[Module].department.name should be ("Computer Science")
    }
    
    @Ignore @Test def getAllCommands {
    	allCommandsInSystem("uk.ac.warwick.tabula.coursework").map {clz => println(clz.getName.substring("uk.ac.warwick.tabula.coursework".length))}
    }
    
}
