package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.AppContextTestBase
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.MockUserLookup
import org.junit.Before
import uk.ac.warwick.tabula.Fixtures

class ModuleAndDepartmentServiceTest extends AppContextTestBase {
	
	@Autowired var service: ModuleAndDepartmentService = _
	
	val userLookup = new MockUserLookup
	
	@Before def wire {
		service.userLookup = userLookup
	}
	
	@Test def crud = transactional { tx =>
		// uses data created in data.sql
		
		val ch = service.getDepartmentByCode("ch").get
		val cs = service.getDepartmentByCode("cs").get
		val cssub = service.getDepartmentByCode("cs-subsidiary").get
		
		val cs108 = service.getModuleByCode("cs108").get
		val cs240 = service.getModuleByCode("cs240").get
		val cs241 = service.getModuleByCode("cs241").get
		
		service.allDepartments should be (Seq(ch, cs, cssub))
		service.allModules should be (Seq(cs108, cs240, cs241))
		
		// behaviour of child/parent departments
		cs.children.toArray should be (Array(cssub))
		cssub.parent should be (cs)
		ch.children.isEmpty should be (true)
		cs241.department should be (cssub)
		
		service.getDepartmentByCode("ch") should be (Some(ch))
		service.getDepartmentById(ch.id) should be (Some(ch))
		service.getDepartmentByCode("wibble") should be (None)
		service.getDepartmentById("wibble") should be (None)
		
		service.getModuleByCode("cs108") should be (Some(cs108))
		service.getModuleById(cs108.id) should be (Some(cs108))
		service.getModuleByCode("wibble") should be (None)
		service.getModuleById("wibble") should be (None)
		
		val route = Fixtures.route("g503", "MEng Computer Science")
		session.save(route)
		
		service.getRouteByCode("g503") should be (Some(route))
		service.getRouteByCode("wibble") should be (None)
		
		withUser("cusebr") { service.departmentsOwnedBy(currentUser) should be (Seq(cs)) }
		withUser("cuscav") { 
			service.departmentsOwnedBy(currentUser) should be (Seq())
			service.modulesAdministratedBy(currentUser) should be (Seq())
			service.modulesAdministratedBy(currentUser, cs) should be (Seq())
			service.modulesAdministratedBy(currentUser, ch) should be (Seq())
		
			service.addOwner(cs, "cuscav")
			service.departmentsOwnedBy(currentUser) should be (Seq(cs))
			service.modulesAdministratedBy(currentUser) should be (Seq(cs108, cs240))
			service.modulesAdministratedBy(currentUser, cs) should be (Seq(cs108, cs240))
			service.modulesAdministratedBy(currentUser, ch) should be (Seq())
			
			service.removeOwner(cs, "cuscav")
			service.departmentsOwnedBy(currentUser) should be (Seq())
			
			service.modulesManagedBy(currentUser) should be (Seq())
			service.modulesManagedBy(currentUser, cs) should be (Seq())
			service.modulesManagedBy(currentUser, ch) should be (Seq())
			
			cs108.managers.addUser("cuscav")
			session.update(cs108)
			
			service.modulesManagedBy(currentUser) should be (Seq(cs108))
			service.modulesManagedBy(currentUser, cs) should be (Seq(cs108))
			service.modulesManagedBy(currentUser, ch) should be (Seq())
		}
	}

}
