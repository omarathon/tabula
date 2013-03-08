package uk.ac.warwick.tabula.data.model.permissions

import uk.ac.warwick.tabula.{TestBase, Fixtures}
import uk.ac.warwick.tabula.roles.BuiltInRoleDefinition
import uk.ac.warwick.tabula.permissions.Permissions

class CustomRoleDefinitionTest extends TestBase {
	
	case object TestBuiltInDefinition extends BuiltInRoleDefinition {
		import Permissions._
		
		GrantsScopelessPermission(Masquerade)
		GrantsScopedPermission(Department.ManageExtensionSettings)
		GrantsGlobalPermission(Module.Create)
	}
	
	@Test def polymorphicGetterAndSetter {
		val crd = new CustomRoleDefinition
		crd.baseRoleDefinition should be (null)
		
		crd.baseRoleDefinition = TestBuiltInDefinition
		crd.baseRoleDefinition should be (TestBuiltInDefinition)
		crd.builtInBaseRoleDefinition should be (TestBuiltInDefinition)
		crd.customBaseRoleDefinition should be (null)
		
		val other = new CustomRoleDefinition
		crd.baseRoleDefinition = other
		crd.baseRoleDefinition should be (other)
		crd.builtInBaseRoleDefinition should be (null)
		crd.customBaseRoleDefinition should be (other)
	}
	
	@Test def getPermissions {
		val crd = new CustomRoleDefinition
		crd.baseRoleDefinition = TestBuiltInDefinition
		
		val dept = Fixtures.department("in")
		
		crd.permissions(Some(dept)) should be (Map(
			Permissions.Department.ManageExtensionSettings -> Some(dept),
			Permissions.Module.Create -> None,
			Permissions.Masquerade -> None
		))
		
		val ro1 = new RoleOverride
		ro1.permission = Permissions.Module.Read
		ro1.overrideType = ro1.Allow
		
		crd.overrides.add(ro1)
		
		crd.permissions(Some(dept)) should be (Map(
			Permissions.Department.ManageExtensionSettings -> Some(dept),
			Permissions.Module.Create -> None,
			Permissions.Module.Read -> Some(dept),
			Permissions.Masquerade -> None
		))
		
		val ro2 = new RoleOverride
		ro2.permission = Permissions.Department.ManageExtensionSettings
		ro2.overrideType = ro2.Deny
		
		crd.overrides.add(ro2)
		
		crd.permissions(Some(dept)) should be (Map(
			Permissions.Module.Create -> None,
			Permissions.Module.Read -> Some(dept),
			Permissions.Masquerade -> None
		))
		
		val crd2 = new CustomRoleDefinition
		crd2.baseRoleDefinition = crd
		
		val ro3 = new RoleOverride
		ro3.permission = Permissions.Department.ManageExtensionSettings
		ro3.overrideType = ro3.Allow
		
		crd2.overrides.add(ro3)
		
		crd2.permissions(Some(dept)) should be (Map(
			Permissions.Department.ManageExtensionSettings -> Some(dept),
			Permissions.Module.Create -> None,
			Permissions.Module.Read -> Some(dept),
			Permissions.Masquerade -> None
		))
	}

}