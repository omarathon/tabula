package uk.ac.warwick.tabula.groups.commands.admin

import org.springframework.validation.{BindException, BindingResult}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{SmallGroupService, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.{Fixtures, ItemNotFoundException, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

class EditSmallGroupSetDefaultPropertiesTest extends TestBase with Mockito {

	private trait CommandTestSupport extends SmallGroupServiceComponent {
		val smallGroupService = mock[SmallGroupService]
	}

	private trait Fixture {
		val module = Fixtures.module("in101", "Introduction to Scala")
		module.id = "moduleId"

		val set = Fixtures.smallGroupSet("IN101 Seminars")
		set.id = "existingId"
		set.module = module
	}

	private trait CommandFixture extends Fixture {
		val command = new EditSmallGroupSetDefaultPropertiesCommandInternal(module, set) with CommandTestSupport
	}

	@Test def apply { new CommandFixture {
		command.defaultWeekRanges = Seq(WeekRange(1, 10))
		command.defaultLocation = "The park"

		command.applyInternal() should be (set)

		set.defaultWeekRanges should be (Seq(WeekRange(1, 10)))
		set.defaultLocation should be (NamedLocation("The park"))

		there was one (command.smallGroupService).saveOrUpdate(set)
	}}

	@Test def resetExistingEvents { new CommandFixture {
		command.defaultWeekRanges = Seq(WeekRange(1, 10))
		command.defaultLocation = "The park"

		val group1 = Fixtures.smallGroup("Group A")
		val group2 = Fixtures.smallGroup("Group B")

		val event1 = Fixtures.smallGroupEvent("Event 1")
		val event2 = Fixtures.smallGroupEvent("Event 2")
		event2.location = MapLocation("ITS WW", "12354")

		val event3 = Fixtures.smallGroupEvent("Event 3")
		event3.weekRanges = Seq(WeekRange(1, 5))

		set.groups.add(group1)
		set.groups.add(group2)

		group1.events.add(event1)
		group1.events.add(event2)
		group2.events.add(event3)

		command.resetExistingEvents = true

		command.applyInternal() should be (set)

		set.defaultWeekRanges should be (Seq(WeekRange(1, 10)))
		set.defaultLocation should be (NamedLocation("The park"))

		event1.weekRanges should be (Seq(WeekRange(1, 10)))
		event1.location should be (NamedLocation("The park"))

		event2.weekRanges should be (Seq(WeekRange(1, 10)))
		event2.location should be (NamedLocation("The park"))

		event3.weekRanges should be (Seq(WeekRange(1, 10)))
		event3.location should be (NamedLocation("The park"))

		there was one (command.smallGroupService).saveOrUpdate(set)
	}}

	@Test def permissions { new Fixture {
		val (theModule, theSet) = (module, set)
		val command = new EditSmallGroupSetDefaultPropertiesPermissions with EditSmallGroupSetDefaultPropertiesCommandState {
			val module = theModule
			val set = theSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		there was one(checking).PermissionCheck(Permissions.SmallGroups.Update, set)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoDepartment {
		val command = new EditSmallGroupSetDefaultPropertiesPermissions with EditSmallGroupSetDefaultPropertiesCommandState {
			val module = null
			val set = new SmallGroupSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoSet {
		val command = new EditSmallGroupSetDefaultPropertiesPermissions with EditSmallGroupSetDefaultPropertiesCommandState {
			val module = Fixtures.module("in101")
			val set = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsUnlinkedSet {
		val command = new EditSmallGroupSetDefaultPropertiesPermissions with EditSmallGroupSetDefaultPropertiesCommandState {
			val module = Fixtures.module("in101")
			module.id = "set id"

			val set = new SmallGroupSet(Fixtures.module("other"))
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	private trait ValidationFixture extends Fixture {
		val command = new EditSmallGroupSetDefaultPropertiesValidation with EditSmallGroupSetDefaultPropertiesCommandState {
			val module = ValidationFixture.this.module
			val set = ValidationFixture.this.set
		}
	}

	@Test def validationPasses { new ValidationFixture {
		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validationInvalidLocation { new ValidationFixture {
		command.defaultLocation = "Location with | a pipe"

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("defaultLocation")
		errors.getFieldError.getCodes should contain ("smallGroupEvent.location.invalidChar")
	}}

	@Test def describe { new Fixture {
		val (mod, s) = (module, set)
		val command = new EditSmallGroupSetDefaultPropertiesDescription with EditSmallGroupSetDefaultPropertiesCommandState {
			override val eventName = "test"
			val module = mod
			val set = s
		}

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"module" -> "moduleId",
			"smallGroupSet" -> "existingId"
		))
	}}

	@Test def wires { new Fixture {
		val command = EditSmallGroupSetDefaultPropertiesCommand(module, set)

		command should be (anInstanceOf[Appliable[Seq[SmallGroupSet]]])
		command should be (anInstanceOf[Describable[Seq[SmallGroupSet]]])
		command should be (anInstanceOf[EditSmallGroupSetDefaultPropertiesPermissions])
		command should be (anInstanceOf[EditSmallGroupSetDefaultPropertiesCommandState])
		command should be (anInstanceOf[SelfValidating])
	}}

}
