package uk.ac.warwick.tabula.permissions

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.helpers.ReflectionHelper
import uk.ac.warwick.tabula.permissions.Permissions._

class PermissionTest extends TestBase {

  @Test def of: Unit = {
    Permissions.of("Module.ManageAssignments") match {
      case Module.ManageAssignments =>
      case what: Any => fail("what is this?" + what)
    }
  }

  @Test(expected = classOf[IllegalArgumentException]) def invalidAction: Unit = {
    Permissions.of("Spank")
  }

  @Test(expected = classOf[IllegalArgumentException]) def obsoleteProfilesRead: Unit = {
    // this perm made obsolete (as leaf node) in TAB-564
    Permissions.of("Profiles.Read")
  }

  @Test def name: Unit = {
    Permissions.Assignment.Archive.getName should be("Assignment.Archive")
    Permissions.GodMode.getName should be("GodMode")
    Permissions.of("Module.ManageAssignments").getName should be("Module.ManageAssignments")
  }

  // try and pick up broken equals/hashcode methods
  @Test
  def permissionsCanBeStoredInAHashSet(): Unit = {
    val allperms = ReflectionHelper.allPermissions
    val permsInASet = allperms.toSet
    permsInASet.size should be(allperms.size)

    // will throw ENFE if any element can't be retrieved
    allperms.forall(permsInASet(_)) should be(true)

    val permsAddedTwiceToSet = allperms.foldLeft(permsInASet)((set, perm) => set + perm)
    permsAddedTwiceToSet.size should be(allperms.size)
  }

  @Test
  def anyPermissionSelectorHashing(): Unit = {
    val any = PermissionsSelector.Any[StudentRelationshipType]
    val anyMore = PermissionsSelector.Any[StudentRelationshipType]
    any should be(anyMore)
    Set(any)(anyMore) should be(true)
  }

  @Test
  def selectorPermissionsHashing(): Unit = {
    val selectorPermission = Permissions.Profiles.MeetingRecord.Manage(PermissionsSelector.Any[StudentRelationshipType])
    val selectorPermission2 = Permissions.Profiles.MeetingRecord.Manage(PermissionsSelector.Any[StudentRelationshipType])

    Set(selectorPermission)(selectorPermission2) should be(true)
    selectorPermission should be(selectorPermission2)
  }

}