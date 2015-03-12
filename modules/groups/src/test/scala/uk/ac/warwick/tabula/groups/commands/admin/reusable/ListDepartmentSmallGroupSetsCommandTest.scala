package uk.ac.warwick.tabula.groups.commands.admin.reusable

import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{SmallGroupService, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.{ItemNotFoundException, Fixtures, TestBase, Mockito}

class ListDepartmentSmallGroupSetsCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends SmallGroupServiceComponent {
		val smallGroupService = mock[SmallGroupService]
	}

	private trait Fixture {
		val department = Fixtures.department("in", "IT Services")

		val set1 = new DepartmentSmallGroupSet(department)
		set1.name = "Set 1"

		val set2 = new DepartmentSmallGroupSet(department)
		set2.name = "Set 2"
	}

	private trait CommandFixture extends Fixture {
		val command = new ListDepartmentSmallGroupSetsCommandInternal(department) with CommandTestSupport
	}

	@Test def apply { new CommandFixture {
		command.smallGroupService.getDepartmentSmallGroupSets(department) returns (Seq(set1, set2))

		command.applyInternal() should be (Seq(set1, set2))
	}}

	@Test def permissions {
		val command = new ListDepartmentSmallGroupSetsPermissions with ListDepartmentSmallGroupSetsCommandState {
			override val department = Fixtures.department("in")
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Create, command.department)
	}

	@Test(expected = classOf[ItemNotFoundException]) def noDepartment {
		val command = new ListDepartmentSmallGroupSetsPermissions with ListDepartmentSmallGroupSetsCommandState {
			override val department = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test def wiresTogether { new Fixture {
		val command = ListDepartmentSmallGroupSetsCommand(department)

		command should be (anInstanceOf[Appliable[Seq[DepartmentSmallGroupSet]]])
		command should be (anInstanceOf[ListDepartmentSmallGroupSetsPermissions])
		command should be (anInstanceOf[ListDepartmentSmallGroupSetsCommandState])
	}}

}
