package uk.ac.warwick.tabula.scheduling.services

import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasProperty
import org.junit.Before
import org.junit.Test
import org.specs.mock.JMocker.{expect => expecting}
import org.specs.mock.JMocker.mock
import org.specs.mock.JMocker.one
import org.specs.mock.JMocker.returnValue
import org.specs.mock.JMocker.will

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService

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