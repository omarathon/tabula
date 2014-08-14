package uk.ac.warwick.tabula.groups.commands.admin

import org.springframework.validation.{BindException, BindingResult}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroupSet, SmallGroup}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{SmallGroupService, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.{ItemNotFoundException, Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._

class EditSmallGroupsCommandTest extends TestBase with Mockito {

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

	private trait ExistingGroupsFixture extends Fixture {
		val groupA = Fixtures.smallGroup("Group A")
		val groupB = Fixtures.smallGroup("Group B")
		val groupC = Fixtures.smallGroup("Group C")
		val groupD = Fixtures.smallGroup("Group D")

		set.groups.add(groupA)
		set.groups.add(groupB)
		set.groups.add(groupC)
		set.groups.add(groupD)
	}

	private trait CommandFixture extends Fixture {
		val command = new EditSmallGroupsCommandInternal(module, set) with CommandTestSupport with PopulateEditSmallGroupsCommand
	}

	private trait CommandWithExistingFixture extends ExistingGroupsFixture {
		val command = new EditSmallGroupsCommandInternal(module, set) with CommandTestSupport with PopulateEditSmallGroupsCommand with EditSmallGroupsCommandRemoveTrailingEmptyGroups
		command.populate()
	}

	@Test def usesDefaultMaxGroupSizeIfEnabled { new CommandFixture {
		set.defaultMaxGroupSize = 8
		set.defaultMaxGroupSizeEnabled = true

		command.populate()

		command.defaultMaxGroupSizeEnabled should be (true)
		command.defaultMaxGroupSize should be (8)
	}}

	@Test def populate { new CommandFixture {
		set.groups.add(Fixtures.smallGroup("Group A"))
		set.groups.add(Fixtures.smallGroup("Group B"))
		set.defaultMaxGroupSize = 8
		set.defaultMaxGroupSizeEnabled = true

		command.populate()

		command.groupNames.asScala should be (Seq("Group A", "Group B"))
		command.maxGroupSizes.asScala should be (Seq(8, 8))
	}}

	@Test def create { new CommandFixture {
		command.populate()
		command.groupNames.add("Group A")
		command.groupNames.add("Group B")
		command.maxGroupSizes.add(15)
		command.maxGroupSizes.add(15)
		command.defaultMaxGroupSizeEnabled = true

		val groups = command.applyInternal()
		set.groups.asScala should be (groups)

		groups(0).name should be ("Group A")
		groups(0).maxGroupSize should be (Some(15))

		groups(1).name should be ("Group B")
		groups(1).maxGroupSize should be (Some(15))

		there was one (command.smallGroupService).saveOrUpdate(set)
	}}

	@Test def edit { new CommandWithExistingFixture {
		command.groupNames.asScala should be (Seq("Group A", "Group B", "Group C", "Group D"))
		command.groupNames.set(1, "Edited group")
		command.groupNames.set(3, "")

		command.onBind(mock[BindingResult])

		val groups = command.applyInternal()
		groups should be (Seq(groupA, groupB, groupC))

		groupA.name should be ("Group A")
		groupB.name should be ("Edited group")
		groupC.name should be ("Group C")

		set.groups.asScala should be (groups)

		there was one (command.smallGroupService).saveOrUpdate(set)
	}}

	@Test def permissions { new Fixture {
		val (theModule, theSet) = (module, set)
		val command = new EditSmallGroupsPermissions with EditSmallGroupsCommandState {
			val module = theModule
			val set = theSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		there was one(checking).PermissionCheck(Permissions.SmallGroups.Update, set)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoDepartment {
		val command = new EditSmallGroupsPermissions with EditSmallGroupsCommandState {
			val module = null
			val set = new SmallGroupSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoSet {
		val command = new EditSmallGroupsPermissions with EditSmallGroupsCommandState {
			val module = Fixtures.module("in101")
			val set = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsUnlinkedSet {
		val command = new EditSmallGroupsPermissions with EditSmallGroupsCommandState {
			val module = Fixtures.module("in101")
			module.id = "set id"

			val set = new SmallGroupSet(Fixtures.module("other"))
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	private trait ValidationFixture extends ExistingGroupsFixture {
		val command = new EditSmallGroupsValidation with EditSmallGroupsCommandState with PopulateEditSmallGroupsCommand {
			val module = ValidationFixture.this.module
			val set = ValidationFixture.this.set
		}
		command.populate()
	}

	@Test def validationPasses { new ValidationFixture {
		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validationAddNewPasses { new ValidationFixture {
		command.groupNames.add("Group E")

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validationEditPasses { new ValidationFixture {
		command.groupNames.set(1, "Edited group")
		command.groupNames.remove(3)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateLinked { new ValidationFixture {
		set.allocationMethod = SmallGroupAllocationMethod.Linked

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getGlobalError.getCodes should contain ("smallGroupSet.linked")
	}}

	@Test def validateNoName { new ValidationFixture {
		command.groupNames.set(1, "             ")

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("groupNames[1]")
		errors.getFieldError.getCodes should contain ("smallGroup.name.NotEmpty")
	}}

	@Test def validateNameTooLong { new ValidationFixture {
		command.groupNames.set(1, (1 to 300).map { _ => "a" }.mkString(""))

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("groupNames[1]")
		errors.getFieldError.getCodes should contain ("smallGroup.name.Length")
	}}

	@Test def validateCantRemoveNonEmpty { new ValidationFixture {
		groupD.students.add(new User("cuscav") {{ setWarwickId("0672089") }})
		command.groupNames.remove(3)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("groupNames[3]")
		errors.getFieldError.getCodes should contain ("smallGroup.delete.notEmpty")
	}}

	@Test def describe { new Fixture {
		val (mod, s) = (module, set)
		val command = new EditSmallGroupsDescription with EditSmallGroupsCommandState {
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
		val command = EditSmallGroupsCommand(module, set)

		command should be (anInstanceOf[Appliable[Seq[SmallGroup]]])
		command should be (anInstanceOf[Describable[Seq[SmallGroup]]])
		command should be (anInstanceOf[EditSmallGroupsPermissions])
		command should be (anInstanceOf[EditSmallGroupsCommandState])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[PopulateOnForm])
		command should be (anInstanceOf[BindListener])
	}}

}
