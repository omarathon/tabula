package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.MockUserLookup
import org.junit.Before
import uk.ac.warwick.tabula.Fixtures

class ModuleAndDepartmentServiceTest extends AppContextTestBase {
	
	lazy val service = Wire[ModuleAndDepartmentService]
	
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
		
		service.departmentsOwnedBy("cusebr") should be (Seq(cs))
		service.departmentsOwnedBy("cuscav") should be (Seq())
		service.modulesAdministratedBy("cuscav") should be (Seq())
		service.modulesAdministratedBy("cuscav", cs) should be (Seq())
		service.modulesAdministratedBy("cuscav", ch) should be (Seq())
		
		service.addOwner(cs, "cuscav")
		service.departmentsOwnedBy("cuscav") should be (Seq(cs))
		service.modulesAdministratedBy("cuscav") should be (Seq(cs108, cs240))
		service.modulesAdministratedBy("cuscav", cs) should be (Seq(cs108, cs240))
		service.modulesAdministratedBy("cuscav", ch) should be (Seq())
		
		service.removeOwner(cs, "cuscav")
		service.departmentsOwnedBy("cuscav") should be (Seq())
		
		service.modulesManagedBy("cuscav") should be (Seq())
		service.modulesManagedBy("cuscav", cs) should be (Seq())
		service.modulesManagedBy("cuscav", ch) should be (Seq())
		
		cs108.managers.addUser("cuscav")
		session.update(cs108)
		
		service.modulesManagedBy("cuscav") should be (Seq(cs108))
		service.modulesManagedBy("cuscav", cs) should be (Seq(cs108))
		service.modulesManagedBy("cuscav", ch) should be (Seq())
		
	}

}
