package uk.ac.warwick.tabula.data.model.permissions

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.roles._

class BuiltInRoleDefinitionUserTypeTest extends TestBase {
  
	@Test def convertToObject() {
		val t = new BuiltInRoleDefinitionUserType
		t.convertToObject("DepartmentalAdministratorRoleDefinition") should be (DepartmentalAdministratorRoleDefinition)
		t.convertToObject("ModuleManagerRoleDefinition") should be (ModuleManagerRoleDefinition)
		t.convertToObject("SettingsOwnerRoleDefinition") should be (SettingsOwnerRoleDefinition)
		t.convertToObject("SubmitterRoleDefinition") should be (SubmitterRoleDefinition)
		evaluating { t.convertToObject("Q") } should produce [IllegalArgumentException]
	}
  
	@Test def convertToValue() {
		val t = new BuiltInRoleDefinitionUserType
		t.convertToValue(DepartmentalAdministratorRoleDefinition) should be ("DepartmentalAdministratorRoleDefinition")
		t.convertToValue(ModuleManagerRoleDefinition) should be ("ModuleManagerRoleDefinition")
		t.convertToValue(SettingsOwnerRoleDefinition) should be ("SettingsOwnerRoleDefinition")
		t.convertToValue(SubmitterRoleDefinition) should be ("SubmitterRoleDefinition")
	}

}