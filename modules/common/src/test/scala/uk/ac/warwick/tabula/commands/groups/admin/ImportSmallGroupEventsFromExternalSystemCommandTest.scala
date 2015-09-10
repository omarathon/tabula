package uk.ac.warwick.tabula.commands.groups.admin

import org.joda.time.LocalTime
import org.junit.Before
import uk.ac.warwick.spring.SpringConfigurer
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.DescriptionImpl
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model.{Location, Module, NamedLocation}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.{ModuleTimetableFetchingService, ModuleTimetableFetchingServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.timetables.TimetableEvent.Parent
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}
import uk.ac.warwick.userlookup.User

import scala.util.Success

class ImportSmallGroupEventsFromExternalSystemCommandTest extends TestBase with Mockito {

	@Before def tidyUpContext() {
		// TODO it would be better to find where this context is actually coming from
		SpringConfigurer.applicationContext = null
	}

	val timetableFetchingService = mock[ModuleTimetableFetchingService]
	val smallGroupService = mock[SmallGroupService]
	val userLookup = new MockUserLookup

	private trait MockServices extends ModuleTimetableFetchingServiceComponent
		with SmallGroupServiceComponent
		with UserLookupComponent {

		val timetableFetchingService = ImportSmallGroupEventsFromExternalSystemCommandTest.this.timetableFetchingService
		val smallGroupService = ImportSmallGroupEventsFromExternalSystemCommandTest.this.smallGroupService
		val userLookup = ImportSmallGroupEventsFromExternalSystemCommandTest.this.userLookup

	}

	private trait CommandTestSupport extends SmallGroupEventGenerator {
		self: MockServices with ImportSmallGroupEventsFromExternalSystemCommandState =>

		def createEvent(module: Module, set: SmallGroupSet, group: SmallGroup, weeks: Seq[WeekRange], day: DayOfWeek, startTime: LocalTime, endTime: LocalTime, location: Option[Location], tutorUsercodes: Seq[String]) = {
			val event = new SmallGroupEvent(group)
			updateEvent(module, set, group, event, weeks, day, startTime, endTime, location, tutorUsercodes)

			group.addEvent(event)
			event
		}

		def updateEvent(module: Module, set: SmallGroupSet, group: SmallGroup, event: SmallGroupEvent, weeks: Seq[WeekRange], day: DayOfWeek, startTime: LocalTime, endTime: LocalTime, location: Option[Location], tutorUsercodes: Seq[String]) = {
			event.weekRanges = weeks
			event.day = day
			event.startTime = startTime
			event.endTime = endTime
			location.foreach { location => event.location = location }
			event.tutors.knownType.includedUserIds = tutorUsercodes

			event
		}
	}

	private trait Fixture {
		val module = Fixtures.module("in101")
		val set = Fixtures.smallGroupSet("IN101 Seminars")
		val group1 = Fixtures.smallGroup("Group 1")
		val group2 = Fixtures.smallGroup("Group 2")
	}

	private trait CommandFixture extends Fixture {
		val command =
			new ImportSmallGroupEventsFromExternalSystemCommandInternal(module, set)
				with CommandTestSupport
				with MockServices
				with LookupEventsFromModuleTimetable
	}

	private trait FixtureWithSingleSeminarForYear extends Fixture with MockServices {
		set.academicYear = AcademicYear(2012)

		val tEventSeminar1 = TimetableEvent(
			uid="uuid1",
			name="IN101S",
			title="",
			description="",
			startTime=new LocalTime(12, 0),
			endTime=new LocalTime(13, 0),
			weekRanges=Seq(WeekRange(6, 10)),
			day=DayOfWeek.Friday,
			eventType=TimetableEventType.Seminar,
			location=Some(NamedLocation("CS1.04")),
			parent=Parent.Module(Some(module)),
			comments=None,
			staffUniversityIds=Seq("1170047"),
			studentUniversityIds=Seq("0000001", "0000002", "0000003"),
			year = AcademicYear(2012)
		)
		val tEventSeminar2 = TimetableEvent(
			uid="uuid2",
			name="IN101S",
			title="",
			description="",
			startTime=new LocalTime(12, 0),
			endTime=new LocalTime(13, 0),
			weekRanges=Seq(WeekRange(6, 10)),
			day=DayOfWeek.Thursday,
			eventType=TimetableEventType.Seminar,
			location=Some(NamedLocation("CS1.04")),
			parent=Parent.Module(Some(module)),
			comments=None,
			staffUniversityIds=Seq("1170047"),
			studentUniversityIds=Seq("0000004", "0000005", "0000006"),
			year = AcademicYear(2012)
		)

		timetableFetchingService.getTimetableForModule("IN101") returns Success(Seq(
			tEventSeminar1, tEventSeminar2,
			TimetableEvent(
				uid="uuid3",
				name="IN101L",
				title="",
				description="",
				startTime=new LocalTime(12, 0),
				endTime=new LocalTime(13, 0),
				weekRanges=Seq(WeekRange(6, 10)),
				day=DayOfWeek.Friday,
				eventType=TimetableEventType.Lecture,
				location=Some(NamedLocation("L5")),
				parent=Parent.Module(Some(module)),
				comments=None,
				staffUniversityIds=Seq("1170047"),
				studentUniversityIds=Nil,
				year = AcademicYear(2012)
			)
		))

		val tutor = new User("abcdef")
		tutor.setFoundUser(true)
		tutor.setWarwickId("1170047")
		userLookup.registerUserObjects(tutor)
	}

	@Test def init() { new FixtureWithSingleSeminarForYear with CommandFixture {
		command.eventsToImport.size() should be (2)
		command.eventsToImport.get(0).timetableEvent should be (tEventSeminar2)
		command.eventsToImport.get(1).timetableEvent should be (tEventSeminar1)
	}}

	@Test def apply() { new FixtureWithSingleSeminarForYear with CommandFixture {
		command.eventsToImport.get(0).group = group1
		command.eventsToImport.get(1).group = group2

		val sets = command.applyInternal()

		verify(command.smallGroupService, times(1)).saveOrUpdate(set)

		val group1event = group1.events.head
		group1event.weekRanges should be (Seq(WeekRange(6, 10)))
		group1event.day should be (DayOfWeek.Thursday)
		group1event.startTime should be (new LocalTime(12, 0))
		group1event.endTime should be (new LocalTime(13, 0))
		group1event.location should be (NamedLocation("CS1.04"))
		group1event.tutors.knownType.includedUserIds should be (Seq("abcdef"))

		val group2event = group2.events.head
		group2event.weekRanges should be (Seq(WeekRange(6, 10)))
		group2event.day should be (DayOfWeek.Friday)
		group2event.startTime should be (new LocalTime(12, 0))
		group2event.endTime should be (new LocalTime(13, 0))
		group2event.location should be (NamedLocation("CS1.04"))
		group2event.tutors.knownType.includedUserIds should be (Seq("abcdef"))
	}}

	private trait PermissionsFixture extends Fixture {
		val command = new ImportSmallGroupEventsFromExternalSystemPermissions with ImportSmallGroupEventsFromExternalSystemCommandState {
			val module = PermissionsFixture.this.module
			val set = PermissionsFixture.this.set
		}
	}

	@Test def permissions() { new PermissionsFixture {
		module.groupSets.add(set)
		set.module = module

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Update, set)
	}}

	@Test(expected = classOf[ItemNotFoundException])
	def notLinked() { new PermissionsFixture {
		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		fail("Expected exception")
	}}

	@Test def description() {
		val command = new ImportSmallGroupEventsFromExternalSystemDescription with ImportSmallGroupEventsFromExternalSystemCommandState {
			override val eventName: String = "test"
			val module = Fixtures.module("in101")
			val set = Fixtures.smallGroupSet("IN101 Seminars")

			module.id = "moduleId"
			set.id = "setId"

			module.groupSets.add(set)
			set.module = module
		}

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"smallGroupSet" -> "setId",
			"module" -> "moduleId"
		))
	}

}
