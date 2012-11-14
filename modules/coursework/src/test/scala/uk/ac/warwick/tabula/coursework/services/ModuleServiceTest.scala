package uk.ac.warwick.tabula.coursework.services
import uk.ac.warwick.tabula.coursework.data.model.Department
import uk.ac.warwick.tabula.coursework.data.DepartmentDao
import org.specs.mock.JMocker._
import org.specs.mock.JMocker.{mock => jmock}
import org.specs.mock.JMocker.{expect => expecting}
import org.hamcrest.BaseMatcher
import org.hamcrest.Matchers._
import org.junit.Before
import org.scalatest.junit.ShouldMatchersForJUnit
import org.junit.Test
import uk.ac.warwick.tabula.coursework.TestBase



class ModuleServiceTest extends TestBase {
  
    var moduleService:ModuleAndDepartmentService = null;
  
    @Before def before {
		moduleService = new ModuleAndDepartmentService
    }
    
    @Test def moduleServiceImport {
         
//         val dao = jmock[DepartmentDao]
//         expecting {
//			  one(dao).save(departmentLike("ch","Chemistry"))
//			  one(dao).save(departmentLike("ph","Physics"))
//			  never(dao).save(departmentLike("in","IT Services"))
//			  
//			  allowing(dao).getByCode(anyString)
//			  will(returnValue(None))
//		 }
         
		 //moduleService.moduleImporter = mockModuleImporter
		 //moduleService.departmentDao = dao
		 //moduleService.importDepartments
	  
    }
    
    def departmentLike(code:String,name:String):Department = withArg(
			      hasProperty("code",equalTo(code)),
			      hasProperty("name",equalTo(name))
			  )
    
	def mockModuleImporter = {
	  val fetcher = mock[ModuleImporter]
	  expecting {
	    one(fetcher).getDepartments
	    will(returnValue(List(
	        new DepartmentInfo("Physics","ph","Science"),
	        new DepartmentInfo("IT Services","in","Service/Admin"),
	        new DepartmentInfo("Chemistry","ch","Science")
	    )))
	  }
	  fetcher
	}
}