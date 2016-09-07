package uk.ac.warwick.tabula.commands.groups.admin.reusable

import org.springframework.validation.{BindException, BindingResult}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroup, DepartmentSmallGroupSet, SmallGroupEventAttendance, SmallGroupEventOccurrence}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{SmallGroupService, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

class EditDepartmentSmallGroupsCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends SmallGroupServiceComponent {
		val smallGroupService = mock[SmallGroupService]
	}

	private trait Fixture {
		val department = Fixtures.department("in", "IT Services")
		val set = new DepartmentSmallGroupSet(department)
		set.id = "existingId"
		set.name = "Existing set"
	}

	private trait ExistingGroupsFixture extends Fixture {
		val groupA = new DepartmentSmallGroup(set) {
			name = "Group A"
		}
		val groupB = new DepartmentSmallGroup(set) {
			name = "Group B"
		}
		val groupC = new DepartmentSmallGroup(set) {
			name = "Group C"
		}
		val groupD = new DepartmentSmallGroup(set) {
			name = "Group D"
		}

		set.groups.add(groupA)
		set.groups.add(groupB)
		set.groups.add(groupC)
		set.groups.add(groupD)
	}

	private trait CommandFixture extends Fixture {
		val command = new EditDepartmentSmallGroupsCommandInternal(department, set) with CommandTestSupport
	}

	private trait CommandWithExistingFixture extends ExistingGroupsFixture {
		val command = new EditDepartmentSmallGroupsCommandInternal(department, set) with CommandTestSupport with PopulateEditDepartmentSmallGroupsCommand with EditDepartmentSmallGroupsCommandRemoveTrailingEmptyGroups
		command.populate()
	}

	@Test def create() { new CommandFixture {
		command.groupNames.add("Group A")
		command.groupNames.add("Group B")
		command.groupNames.add("Group C")
		command.groupNames.add("Group D")
		command.groupNames.add("Group E")

		val groups = command.applyInternal()
		groups.size should be (5)

		groups.head.name should be ("Group A")
		groups(1).name should be ("Group B")
		groups(2).name should be ("Group C")
		groups(3).name should be ("Group D")
		groups(4).name should be ("Group E")

		set.groups.asScala should be (groups)

		verify(command.smallGroupService, times(1)).saveOrUpdate(set)
	}}

	@Test def edit() { new CommandWithExistingFixture {
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

		verify(command.smallGroupService, times(1)).saveOrUpdate(set)
	}}

	@Test def permissions() { new Fixture {
		val (theDepartment, theSet) = (department, set)
		val command = new EditDepartmentSmallGroupsPermissions with EditDepartmentSmallGroupsCommandState {
			val department = theDepartment
			val set = theSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Update, set)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoDepartment() {
		val command = new EditDepartmentSmallGroupsPermissions with EditDepartmentSmallGroupsCommandState {
			val department = null
			val set = new DepartmentSmallGroupSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoSet() {
		val command = new EditDepartmentSmallGroupsPermissions with EditDepartmentSmallGroupsCommandState {
			val department = Fixtures.department("in")
			val set = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsUnlinkedSet() {
		val command = new EditDepartmentSmallGroupsPermissions with EditDepartmentSmallGroupsCommandState {
			val department = Fixtures.department("in")
			department.id = "set id"

			val set = new DepartmentSmallGroupSet(Fixtures.department("other"))
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	private trait ValidationFixture extends ExistingGroupsFixture {
		val command = new EditDepartmentSmallGroupsValidation with EditDepartmentSmallGroupsCommandState
			with PopulateEditDepartmentSmallGroupsCommand with CommandTestSupport {
			val department = ValidationFixture.this.department
			val set = ValidationFixture.this.set
		}
		command.populate()
	}

	@Test def validationPasses() { new ValidationFixture {
		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validationAddNewPasses() { new ValidationFixture {
		command.groupNames.add("Group E")

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validationEditPasses() { new ValidationFixture {
		command.groupNames.set(1, "Edited group")
		command.groupNames.remove(3)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateNoName() { new ValidationFixture {
		command.groupNames.set(1, "             ")

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("groupNames[1]")
		errors.getFieldError.getCodes should contain ("smallGroup.name.NotEmpty")
	}}

	@Test def validateNameTooLong() { new ValidationFixture {
		command.groupNames.set(1, (1 to 300).map { _ => "a" }.mkString(""))

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("groupNames[1]")
		errors.getFieldError.getCodes should contain ("smallGroup.name.Length")
	}}

	@Test def validateCantRemoveNonEmpty() { new ValidationFixture {
		groupD.students.add(new User("cuscav") {{ setWarwickId("0672089") }})
		command.groupNames.remove(3)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("groupNames[3]")
		errors.getFieldError.getCodes should contain ("smallGroup.delete.notEmpty")
	}}

	@Test def validateCantRemoveWhenAttendanceRecorded() { new ValidationFixture {
		val event = Fixtures.smallGroupEvent("An Event")
		val linkedGroup = Fixtures.smallGroup("Linked group")
		groupD.linkedGroups.add(linkedGroup)
		linkedGroup.addEvent(event)

		command.groupNames.remove(3)

		val eventOccurrence = new SmallGroupEventOccurrence
		eventOccurrence.event = event

		val missedAuthorisedAttendance = new SmallGroupEventAttendance
		missedAuthorisedAttendance.occurrence = eventOccurrence
		missedAuthorisedAttendance.state = AttendanceState.MissedAuthorised

		eventOccurrence.attendance.add(missedAuthorisedAttendance)

		command.smallGroupService.getAllSmallGroupEventOccurrencesForEvent(event) returns Seq(eventOccurrence)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("groupNames[3]")
		errors.getFieldError.getCodes should contain ("smallGroupEvent.delete.hasAttendance")
	}}

	@Test def validateCanRemoveWhenAttendaneNotRecorded() { new ValidationFixture {
		val event = Fixtures.smallGroupEvent("An Event")
		val linkedGroup = Fixtures.smallGroup("Linked group")
		groupD.linkedGroups.add(linkedGroup)
		linkedGroup.addEvent(event)

		command.groupNames.remove(3)

		val eventOccurrence = new SmallGroupEventOccurrence
		eventOccurrence.event = event

		val notRecordedAttendance = new SmallGroupEventAttendance
		notRecordedAttendance.occurrence = eventOccurrence
		notRecordedAttendance.state = AttendanceState.NotRecorded

		eventOccurrence.attendance.add(notRecordedAttendance)

		command.smallGroupService.getAllSmallGroupEventOccurrencesForEvent(event) returns Seq(eventOccurrence)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def describe() { new Fixture {
		val (dept, s) = (department, set)
		val command = new EditDepartmentSmallGroupsDescription with EditDepartmentSmallGroupsCommandState {
			override val eventName = "test"
			val department = dept
			val set = s
		}

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"department" -> "in",
			"smallGroupSet" -> "existingId"
		))
	}}

	@Test def wires() { new Fixture {
		val command = EditDepartmentSmallGroupsCommand(department, set)

		command should be (anInstanceOf[Appliable[Seq[DepartmentSmallGroup]]])
		command should be (anInstanceOf[Describable[Seq[DepartmentSmallGroup]]])
		command should be (anInstanceOf[EditDepartmentSmallGroupsPermissions])
		command should be (anInstanceOf[EditDepartmentSmallGroupsCommandState])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[PopulateOnForm])
		command should be (anInstanceOf[BindListener])
	}}

}
