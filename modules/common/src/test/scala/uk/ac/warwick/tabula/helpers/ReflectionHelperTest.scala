package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.TestBase
import org.reflections.Reflections
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permissions

class ReflectionHelperTest extends TestBase {
	
	new Reflections("uk.ac.warwick.tabula").save(getClass.getResource("/").getFile() + "META-INF/reflections/all-reflections.xml")
	
	@Test def allPermissionsTargets = {
		ReflectionHelper.allPermissionTargets.contains(classOf[Department]) should be (true) 
	}
	
	@Test def allPermissions = {
		ReflectionHelper.allPermissions.contains(Permissions.Module.Read) should be (true) 
	}
	
	@Test def groupedPermissions = {
		ReflectionHelper.groupedPermissions("Module").contains(("Module.Create", "Module.Create")) should be (true) 
	}

}