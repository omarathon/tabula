package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.TestBase
import org.junit.Test

class RoleTest extends TestBase {

  @Test def of: Unit = {
    RoleDefinition.of("DepartmentalAdministratorRoleDefinition") match {
      case DepartmentalAdministratorRoleDefinition =>
      case what: Any => fail("what is this?" + what)
    }
  }

  @Test(expected = classOf[IllegalArgumentException]) def invalidAction: Unit = {
    RoleDefinition.of("Spank")
  }

  @Test def name: Unit = {
    DepartmentalAdministratorRoleDefinition.getName should be("DepartmentalAdministratorRoleDefinition")
    SysadminRoleDefinition.getName should be("SysadminRoleDefinition")
    RoleDefinition.of("SysadminRoleDefinition").getName should be("SysadminRoleDefinition")
  }

}