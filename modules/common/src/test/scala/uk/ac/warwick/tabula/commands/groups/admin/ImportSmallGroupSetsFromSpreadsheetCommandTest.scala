package uk.ac.warwick.tabula.commands.groups.admin

import java.io.ByteArrayInputStream
import java.util.UUID

import org.joda.time.LocalTime
import org.springframework.validation.{BindException, Errors}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model.{FileAttachment, NamedLocation}
import uk.ac.warwick.tabula.services.groups.docconversion._
import uk.ac.warwick.tabula.services.objectstore.ObjectStorageService
import uk.ac.warwick.tabula.services.{MaintenanceModeService, SmallGroupService, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

class ImportSmallGroupSetsFromSpreadsheetCommandTest extends TestBase with Mockito {

	private trait BindingTestSupport extends ImportSmallGroupSetsFromSpreadsheetRequest with SmallGroupSetSpreadsheetHandlerComponent with SmallGroupServiceComponent {
		val smallGroupSetSpreadsheetHandler = smartMock[SmallGroupSetSpreadsheetHandler]
		val smallGroupService = smartMock[SmallGroupService]

		val department = Fixtures.department("in", "IT Services")
		val academicYear = AcademicYear(2015)
	}

	private trait BindingFixture {
		val binding = new ImportSmallGroupSetsFromSpreadsheetBinding with BindingTestSupport
		binding.file.maintenanceMode = smartMock[MaintenanceModeService]
		binding.file.fileDao = smartMock[FileDao]

		val in101 = Fixtures.module("in101", "Introduction to Infrastructure")
		val cuscav = new User("cuscav")
	}

	@Test def bindEmpty(): Unit = new BindingFixture {
		val attachment = new FileAttachment
		attachment.id = UUID.randomUUID().toString
		attachment.name = "file2.xlsx"
		attachment.objectStorageService = smartMock[ObjectStorageService]
		attachment.objectStorageService.keyExists(attachment.id) returns true
		attachment.objectStorageService.metadata(attachment.id) returns Some(ObjectStorageService.Metadata(3, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", None))
		val is = new ByteArrayInputStream("one".getBytes)
		attachment.objectStorageService.fetch(attachment.id) answers { _ => Some(is) }
		binding.file.attached.add(attachment)

		binding.smallGroupSetSpreadsheetHandler.readXSSFExcelFile(isEq(binding.department), isEq(binding.academicYear), isEq(is), isA[BindException]) returns Nil

		val result = new BindException(binding, "command")
		binding.onBind(result)

		result.hasErrors should be (false)
		binding.commands should be (Nil)
	}

	@Test def bind(): Unit = new BindingFixture {
		val attachment = new FileAttachment
		attachment.id = UUID.randomUUID().toString
		attachment.name = "file2.xlsx"
		attachment.objectStorageService = smartMock[ObjectStorageService]
		attachment.objectStorageService.keyExists(attachment.id) returns true
		attachment.objectStorageService.metadata(attachment.id) returns Some(ObjectStorageService.Metadata(3, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", None))
		val is = new ByteArrayInputStream("one".getBytes)
		attachment.objectStorageService.fetch(attachment.id) answers { _ => Some(is) }
		binding.file.attached.add(attachment)

		val extracted = Seq(ExtractedSmallGroupSet(
			module = in101,
			format = SmallGroupFormat.Lab,
			name = "IN101 Labs",
			allocationMethod = SmallGroupAllocationMethod.Manual,
			studentsSeeTutor = true,
			studentsSeeStudents = true,
			studentsCanSwitchGroup = false,
			linkedSmallGroupSet = None,
			collectAttendance = true,
			groups = Seq(
				ExtractedSmallGroup(
					name = "Group 1",
					limit = Some(15),
					events = Seq(
						ExtractedSmallGroupEvent(None, Seq(cuscav), Seq(WeekRange(15, 24)), DayOfWeek.Monday, new LocalTime(14, 0), new LocalTime(16, 0), Some(NamedLocation("S0.27")))
					)
				),
				ExtractedSmallGroup(
					name = "Group 2",
					limit = Some(20),
					events = Seq(
						ExtractedSmallGroupEvent(None, Seq(cuscav), Seq(WeekRange(15, 24)), DayOfWeek.Tuesday, new LocalTime(14, 0), new LocalTime(16, 0), Some(NamedLocation("S0.27")))
					)
				),
				ExtractedSmallGroup(
					name = "Group 3",
					limit = Some(9),
					events = Seq(
						ExtractedSmallGroupEvent(None, Seq(cuscav), Seq(WeekRange(15, 24)), DayOfWeek.Wednesday, new LocalTime(14, 0), new LocalTime(16, 0), Some(NamedLocation("S0.27")))
					)
				),
				ExtractedSmallGroup(
					name = "Group 4",
					limit = Some(15),
					events = Seq(
						ExtractedSmallGroupEvent(None, Seq(cuscav), Seq(WeekRange(15, 24)), DayOfWeek.Thursday, new LocalTime(14, 0), new LocalTime(16, 0), Some(NamedLocation("S0.27")))
					)
				)
			)
		))

		binding.smallGroupSetSpreadsheetHandler.readXSSFExcelFile(isEq(binding.department), isEq(binding.academicYear), isEq(is), isA[BindException]) returns extracted
		binding.smallGroupService.getSmallGroupSets(in101, binding.academicYear) returns Nil

		val result = new BindException(binding, "command")
		binding.onBind(result)

		result.hasErrors should be (false)
		binding.commands should not be 'empty
		binding.commands.length should be (1)

		val commandHolder = binding.commands.head
		commandHolder.commandType should be ("Create")
		commandHolder.command.isInstanceOf[CreateSmallGroupSetCommandInternal] should be (true)

		val setCommand = commandHolder.command.asInstanceOf[CreateSmallGroupSetCommandInternal]
		setCommand.module should be (in101)
		setCommand.format should be (SmallGroupFormat.Lab)
		setCommand.name should be ("IN101 Labs")
		setCommand.allocationMethod should be (SmallGroupAllocationMethod.Manual)
		setCommand.studentsCanSeeTutorName should be (true)
		setCommand.studentsCanSeeOtherMembers should be (true)
		setCommand.allowSelfGroupSwitching should be (false)
		setCommand.linkedDepartmentSmallGroupSet should be (null)
		setCommand.collectAttendance should be (true)

		commandHolder.groupCommands.length should be (4)

		val groupCommandHolder = commandHolder.groupCommands.head
		groupCommandHolder.commandType should be ("Create")
		groupCommandHolder.command.isInstanceOf[CreateSmallGroupCommandInternal] should be (true)

		val groupCommand = groupCommandHolder.command.asInstanceOf[CreateSmallGroupCommandInternal]
		groupCommand.name should be ("Group 1")
		groupCommand.maxGroupSize should be (15)

		groupCommandHolder.eventCommands.length should be (1)

		val eventCommandHolder = groupCommandHolder.eventCommands.head
		eventCommandHolder.commandType should be ("Create")
		eventCommandHolder.command.isInstanceOf[CreateSmallGroupEventCommandInternal] should be (true)

		val eventCommand = eventCommandHolder.command.asInstanceOf[CreateSmallGroupEventCommandInternal]
		eventCommand.title should be (null)
		eventCommand.tutors.asScala should be (Seq("cuscav"))
		eventCommand.weekRanges should be (Seq(WeekRange(15, 24)))
		eventCommand.day should be (DayOfWeek.Monday)
		eventCommand.startTime should be (new LocalTime(14, 0))
		eventCommand.endTime should be (new LocalTime(16, 0))
		eventCommand.location should be ("S0.27")
	}

	class SetCommand(set: SmallGroupSet) extends Appliable[SmallGroupSet] with SelfValidating {
		var called = false
		def apply() = {
			called = true
			set
		}
		def validate(errors: Errors): Unit = {}
	}

	class GroupCommand(group: SmallGroup) extends Appliable[SmallGroup] with SelfValidating {
		var called = false
		def apply() = {
			called = true
			group
		}
		def validate(errors: Errors): Unit = {}
	}

	class EventCommand(event: SmallGroupEvent) extends Appliable[SmallGroupEvent] with SelfValidating {
		var called = false
		def apply() = {
			called = true
			event
		}
		def validate(errors: Errors): Unit = {}
	}

	private trait CommandFixture {
		val department = Fixtures.department("in", "IT Services")
		val academicYear = AcademicYear(2015)

		val command = new ImportSmallGroupSetsFromSpreadsheetCommandInternal(department, academicYear) {}
	}

	@Test def itWorks(): Unit = new CommandFixture {
		val set1 = Fixtures.smallGroupSet("set 1")
		val s1group1 = Fixtures.smallGroup("s1 group 1")
		val s1g1e1 = Fixtures.smallGroupEvent("s1g1e1")
		val s1group2 = Fixtures.smallGroup("s1 group 2")

		val set2 = Fixtures.smallGroupSet("set 2")
		val s2group1 = Fixtures.smallGroup("s1 group 1")
		val s2group2 = Fixtures.smallGroup("s1 group 2")

		command.commands = Seq(
			new ModifySmallGroupSetCommandHolder(new SetCommand(set1), "Create", Seq(
				new ModifySmallGroupCommandHolder(new GroupCommand(s1group1), "Create", Seq(
					new ModifySmallGroupEventCommandHolder(new EventCommand(s1g1e1), "Create")
				)),
				new ModifySmallGroupCommandHolder(new GroupCommand(s1group2), "Create", Nil)
			)),
			new ModifySmallGroupSetCommandHolder(new SetCommand(set2), "Edit", Seq(
				new ModifySmallGroupCommandHolder(new GroupCommand(s2group1), "Create", Nil),
				new ModifySmallGroupCommandHolder(new GroupCommand(s2group2), "Delete", Nil)
			))
		)

		command.applyInternal() should be (Seq(set1, set2))
		command.commands.foreach { sc =>
			sc.command.asInstanceOf[SetCommand].called should be (true)
			sc.groupCommands.foreach { gc =>
				gc.command.asInstanceOf[GroupCommand].called should be (true)
				gc.eventCommands.foreach { ec =>
					ec.command.asInstanceOf[EventCommand].called should be (true)
				}
			}
		}
	}

}
