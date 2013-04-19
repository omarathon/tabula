package uk.ac.warwick.tabula.web.views

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.JavaImports._
import freemarker.template.TemplateModel
import freemarker.template.SimpleHash
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.Fixtures

class PermissionFunctionTest extends TestBase with Mockito {
	
	val fn = new PermissionFunction
	
	val securityService = mock[SecurityService]
	fn.securityService.set(securityService)
	
	@Test def can = withUser("cuscav") {
		val args: JList[TemplateModel] = JArrayList()
		
		val dept = Fixtures.department("in")
		
		// Use a SimpleHash as a workaround to wrapping things manually
		val model = new SimpleHash
		model.put("permissionName", "Module.Create")
		model.put("scope", dept)
		
		args.add(model.get("permissionName"))
		args.add(model.get("scope"))
		
		securityService.can(currentUser, Permissions.Module.Create, dept) returns (true)
		
		fn.exec(args).asInstanceOf[JBoolean] should be (java.lang.Boolean.TRUE)
	}
	
	@Test def cannot = withUser("cuscav") {
		val args: JList[TemplateModel] = JArrayList()
		
		val dept = Fixtures.department("in")
		
		// Use a SimpleHash as a workaround to wrapping things manually
		val model = new SimpleHash
		model.put("permissionName", "Module.Create")
		model.put("scope", dept)
		
		args.add(model.get("permissionName"))
		args.add(model.get("scope"))
		
		securityService.can(currentUser, Permissions.Module.Create, dept) returns (false)
		
		fn.exec(args).asInstanceOf[JBoolean] should be (java.lang.Boolean.FALSE)
	}

}