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
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}
import uk.ac.warwick.userlookup.User

import scala.util.Success

class UpdateSmallGroupEventFromExternalSystemCommandTest extends TestBase with Mockito {

	@Before def tidyUpContext {
		// TODO it would be better to find where this context is actually coming from
		SpringConfigurer.applicationContext = null
	}

	val timetableFetchingService = mock[ModuleTimetableFetchingService]
	val smallGroupService = mock[SmallGroupService]
	val userLookup = new MockUserLookup

	private trait MockServices extends ModuleTimetableFetchingServiceComponent
		with SmallGroupServiceComponent
		with UserLookupComponent {

		val timetableFetchingService = UpdateSmallGroupEventFromExternalSystemCommandTest.this.timetableFetchingService
		val smallGroupService = UpdateSmallGroupEventFromExternalSystemCommandTest.this.smallGroupService
		val userLookup = UpdateSmallGroupEventFromExternalSystemCommandTest.this.userLookup

	}

	private trait CommandTestSupport extends SmallGroupEventUpdater {
		self: MockServices with UpdateSmallGroupEventFromExternalSystemCommandState =>

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
		val group = Fixtures.smallGroup("Group 1")
		val event = Fixtures.smallGroupEvent("Event 1")
	}

	private trait CommandFixture extends Fixture {
		val command =
			new UpdateSmallGroupEventFromExternalSystemCommandInternal(module, set, group, event)
				with CommandTestSupport
				with MockServices
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
			context=Some("IN101"),
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
			context=Some("IN101"),
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
				context=Some("IN101"),
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

	@Test def init { new FixtureWithSingleSeminarForYear with CommandFixture {
		command.timetableEvents should be (Seq(tEventSeminar2, tEventSeminar1))
	}}

	@Test def apply { new FixtureWithSingleSeminarForYear with CommandFixture {
		command.index = 0

		val sets = command.applyInternal()

		verify(command.smallGroupService, times(1)).saveOrUpdate(event)

		event.weekRanges should be (Seq(WeekRange(6, 10)))
		event.day should be (DayOfWeek.Thursday)
		event.startTime should be (new LocalTime(12, 0))
		event.endTime should be (new LocalTime(13, 0))
		event.location should be (NamedLocation("CS1.04"))
		event.tutors.knownType.includedUserIds should be (Seq("abcdef"))
	}}

	private trait PermissionsFixture extends Fixture {
		val command = new UpdateSmallGroupEventFromExternalSystemPermissions with UpdateSmallGroupEventFromExternalSystemCommandState with UpdateSmallGroupEventFromExternalSystemRequestState with MockServices {
			val module = PermissionsFixture.this.module
			val set = PermissionsFixture.this.set
			val group = PermissionsFixture.this.group
			val event = PermissionsFixture.this.event
		}
	}

	@Test def permissions { new PermissionsFixture {
		module.groupSets.add(set)
		set.module = module

		set.groups.add(group)
		group.groupSet = set

		group.addEvent(event)
		event.group = group

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Update, event)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def notLinked { new PermissionsFixture {
		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		fail("Expected exception")
	}}

	@Test def description {
		val command = new UpdateSmallGroupEventFromExternalSystemDescription with UpdateSmallGroupEventFromExternalSystemCommandState with UpdateSmallGroupEventFromExternalSystemRequestState with MockServices {
			override val eventName: String = "test"
			val module = Fixtures.module("in101")
			val set = Fixtures.smallGroupSet("IN101 Seminars")
			val group = Fixtures.smallGroup("Group 1")
			val event = Fixtures.smallGroupEvent("Event 1")

			module.id = "moduleId"
			set.id = "setId"
			group.id = "groupId"
			event.id = "eventId"

			module.groupSets.add(set)
			set.module = module

			set.groups.add(group)
			group.groupSet = set

			group.addEvent(event)
			event.group = group
		}

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"module" -> "moduleId",
			"smallGroupSet" -> "setId",
			"smallGroup" -> "groupId",
			"smallGroupEvent" -> "eventId"
		))
	}

}
