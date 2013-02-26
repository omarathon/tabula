package uk.ac.warwick.tabula.scheduling.commands.imports

import org.hibernate.classic.Session
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.scheduling.services._
import uk.ac.warwick.tabula.services._

class ImportModulesCommandTest extends TestBase with MockitoSugar {

	trait Environment {
		// Test data
		val department = new Department {
			code = "cs"
			name = "Computer Science"
		}
		val moduleInfos = Seq(
            ModuleInfo("Science for Computers", "CS101", null),
            ModuleInfo("Computers for Science", "CS102", null))
        
        // Mocks
		val mockModuleService = mock[ModuleAndDepartmentService]
		val mockSession = mock[Session]
		
		// The class under test, with mocks wired
		val command = new ImportModulesCommand {
			moduleImporter = mock[ModuleImporter]
			moduleService = mockModuleService
			override val session = mockSession
		}
	}

	@Test
	def createModule {
		new Environment {
			when(mockModuleService.getModuleByCode("CS101")) thenReturn None
			when(mockModuleService.getModuleByCode("CS102")) thenReturn None
			
			command.importModules(moduleInfos, department)
			
			verify(mockSession, times(2)).saveOrUpdate(isA[Module])
		}
	}

	// HFC-354
	@Test 
	def updateModuleName { 
		new Environment {
			val existingModule = new Module {
				code = "CS101"
				name = "Brian's fun bus"
			}
			when(mockModuleService.getModuleByCode("CS101")) thenReturn Some(existingModule)
			when(mockModuleService.getModuleByCode("CS102")) thenReturn None

			command.importModules(moduleInfos, department)

			verify(mockSession, times(2)).saveOrUpdate(isA[Module])
			existingModule.name should be ("Science for Computers")
		}
	}
	
	private def isA[A : Manifest] = { 
		org.mockito.Matchers.isA[A](manifest[A].erasure.asInstanceOf[Class[A]])
	}

}