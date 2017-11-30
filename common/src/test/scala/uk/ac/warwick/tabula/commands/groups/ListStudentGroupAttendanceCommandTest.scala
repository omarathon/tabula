package uk.ac.warwick.tabula.commands.groups

import org.joda.time._
import uk.ac.warwick.tabula.commands.groups.SmallGroupAttendanceState._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model.{Member, MemberUserType, UnspecifiedTypeUserGroup, UserGroup}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.termdates.AcademicYearPeriod

class ListStudentGroupAttendanceCommandTest extends TestBase with Mockito {

	val baseLocalDateTime = new DateTime(2014, DateTimeConstants.OCTOBER, 19, 9, 18, 33, 0)

	trait CommandTestSupport extends SmallGroupServiceComponent {
		val smallGroupService: SmallGroupService = smartMock[SmallGroupService]
	}

	trait Fixture {
		val userLookup = new MockUserLookup

		def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
			case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
			case ug: UserGroup => ug.userLookup = userLookup
		}

		val now: DateTime = DateTime.now
		val academicYear: AcademicYear = AcademicYear.forDate(now)

		val set = new SmallGroupSet
		set.academicYear = academicYear
		set.releasedToStudents = true
		set.format = SmallGroupFormat.Seminar

		val group = new SmallGroup(set)
		wireUserLookup(group.students)

		// The group has two events. Event 1 runs at Monday 11am on week 2, 3 and 4; Event 2 runs at Monday 3pm on weeks 1, 3 and 7
		val event1 = new SmallGroupEvent(group)
		event1.day = DayOfWeek.Monday
		event1.startTime = new LocalTime(11, 0)
		event1.endTime = new LocalTime(12, 0)
		event1.weekRanges = Seq(WeekRange(2, 4))

		val event2 = new SmallGroupEvent(group)
		event2.day = DayOfWeek.Monday
		event2.startTime = new LocalTime(15, 0)
		event2.endTime = new LocalTime(16, 0)
		event2.weekRanges = Seq(WeekRange(1), WeekRange(3), WeekRange(7))

		group.addEvent(event1)
		group.addEvent(event2)

		userLookup.registerUsers("user1", "user2", "user3", "user4", "user5")

		val user1: User = userLookup.getUserByUserId("user1")
		val user2: User = userLookup.getUserByUserId("user2")
		val user3: User = userLookup.getUserByUserId("user3")
		val user4: User = userLookup.getUserByUserId("user4")
		val user5: User = userLookup.getUserByUserId("user5")

		group.students.add(user1)
		group.students.add(user2)
		group.students.add(user3)
		group.students.add(user4)

		// user5 turned up to the first occurrence and then left

		// Recorded attendance for week 1 and both in 3 - rest haven't happened yet, 2 is missing
		def attendance(occurrence: SmallGroupEventOccurrence, user: User, state: AttendanceState) {
			val attendance = new SmallGroupEventAttendance
			attendance.occurrence = occurrence
			attendance.universityId = user.getWarwickId
			attendance.state = state
			occurrence.attendance.add(attendance)
		}

		// Everyone turned up for week 1
		val occurrence1 = new SmallGroupEventOccurrence
		occurrence1.id = "occurrence1"
		occurrence1.event = event2
		occurrence1.week = 1
		attendance(occurrence1, user1, AttendanceState.Attended)
		attendance(occurrence1, user2, AttendanceState.Attended)
		attendance(occurrence1, user3, AttendanceState.Attended)
		attendance(occurrence1, user4, AttendanceState.Attended)
		attendance(occurrence1, user5, AttendanceState.Attended)

		// User3 missed the first seminar in week 3, user4 missed the second
		val occurrence2 = new SmallGroupEventOccurrence
		occurrence2.id = "occurrence2"
		occurrence2.event = event1
		occurrence2.week = 3
		attendance(occurrence2, user1, AttendanceState.Attended)
		attendance(occurrence2, user2, AttendanceState.Attended)
		attendance(occurrence2, user3, AttendanceState.MissedUnauthorised)
		attendance(occurrence2, user4, AttendanceState.Attended)
		attendance(occurrence2, user5, AttendanceState.MissedUnauthorised)

		val occurrence3 = new SmallGroupEventOccurrence
		occurrence3.id = "occurrence3"
		occurrence3.event = event2
		occurrence3.week = 3
		attendance(occurrence3, user1, AttendanceState.Attended)
		attendance(occurrence3, user2, AttendanceState.Attended)
		attendance(occurrence3, user3, AttendanceState.Attended)
		attendance(occurrence3, user4, AttendanceState.MissedUnauthorised)
		attendance(occurrence3, user5, AttendanceState.MissedUnauthorised)
	}

	@Test
	def commandApplyNoData() = withFakeTime(baseLocalDateTime) { new Fixture() {
		val member: Member = Fixtures.member(MemberUserType.Student, user1.getWarwickId, user1.getUserId)

		val command = new ListStudentGroupAttendanceCommandInternal(member, academicYear) with CommandTestSupport
		command.smallGroupService.findSmallGroupsByStudent(user1) returns Seq()
		command.smallGroupService.findSmallGroupsWithAttendanceRecorded(user1.getWarwickId) returns Seq()
		command.smallGroupService.findAttendanceNotes(
			Seq(user1).map(_.getWarwickId),
			Seq()
		) returns Seq()

		val info: StudentGroupAttendance = command.applyInternal()
		info.attendance should be ('empty)
		info.missedCount should be (0)
		info.missedCountByTerm should be ('empty)
		info.termWeeks should be ('empty)
	}}

	@Test
	def commandApplyAttendedAll() = withFakeTime(baseLocalDateTime) { new Fixture() { withFakeTime(now) {
		val member = Fixtures.member(MemberUserType.Student, user1.getWarwickId, user1.getUserId)

		val command = new ListStudentGroupAttendanceCommandInternal(member, academicYear) with CommandTestSupport
		command.smallGroupService.findSmallGroupsByStudent(user1) returns Seq(group)
		command.smallGroupService.findSmallGroupsWithAttendanceRecorded(user1.getWarwickId) returns Seq()
		command.smallGroupService.findAttendanceByGroup(group) returns Seq(occurrence1, occurrence2, occurrence3)
		command.smallGroupService.findAttendanceNotes(
			Seq(user1).map(_.getWarwickId),
			Seq(occurrence1, occurrence3, occurrence2)
		) returns Seq()
		
		val autumnTerm = academicYear.termOrVacation(AcademicYearPeriod.PeriodType.autumnTerm)

		val info = command.applyInternal()
		info.missedCount should be (0)
		info.missedCountByTerm should be (Map(autumnTerm -> 0))
		info.termWeeks.toSeq should be (Seq(autumnTerm -> WeekRange(1, 10)))

		// Map all the SortedMaps to Seqs to preserve the order they've been set as
		val attendanceSeqs = info.attendance.toSeq.map { case (t, attendance) =>
			t -> attendance.toSeq.map { case (g, att) =>
				g -> att.toSeq.map { case (w, at) =>
					w -> at.toSeq
				}
			}
		}

		attendanceSeqs should be (Seq(
			(autumnTerm, Seq(
				(group, Seq(
					(1, Seq(((event2, 1), Attended))),
					(2, Seq(((event1, 2), Late))),
					(3, Seq(((event1, 3), Attended), ((event2, 3), Attended))),
					(4, Seq(((event1, 4), NotRecorded))),
					(7, Seq(((event2, 7), NotRecorded)))
				))
			))
		))
	}}}

	@Test
	def commandApplyAttendedMost() = withFakeTime(baseLocalDateTime) { new Fixture() { withFakeTime(now) {
		val member = Fixtures.member(MemberUserType.Student, user3.getWarwickId, user3.getUserId)

		val command = new ListStudentGroupAttendanceCommandInternal(member, academicYear) with CommandTestSupport
		command.smallGroupService.findSmallGroupsByStudent(user3) returns Seq(group)
		command.smallGroupService.findSmallGroupsWithAttendanceRecorded(user3.getWarwickId) returns Seq()
		command.smallGroupService.findAttendanceByGroup(group) returns Seq(occurrence1, occurrence2, occurrence3)
		command.smallGroupService.findAttendanceNotes(
			Seq(user3).map(_.getWarwickId),
			Seq(occurrence1, occurrence3, occurrence2)
		) returns Seq()

		val autumnTerm = academicYear.termOrVacation(AcademicYearPeriod.PeriodType.autumnTerm)

		val info = command.applyInternal()
		info.missedCount should be (1)
		info.missedCountByTerm should be (Map(autumnTerm -> 1))
		info.termWeeks.toSeq should be (Seq(autumnTerm -> WeekRange(1, 10)))

		// Map all the SortedMaps to Seqs to preserve the order they've been set as
		val attendanceSeqs = info.attendance.toSeq.map { case (t, attendance) =>
			t -> attendance.toSeq.map { case (g, att) =>
				g -> att.toSeq.map { case (weekNumber, at) =>
					weekNumber -> at.toSeq
				}
			}
		}

		attendanceSeqs should be (Seq(
			(autumnTerm, Seq(
				(group, Seq(
					(1, Seq(((event2, 1), Attended))),
					(2, Seq(((event1, 2), Late))),
					(3, Seq(((event1, 3), MissedUnauthorised), ((event2, 3), Attended))),
					(4, Seq(((event1, 4), NotRecorded))),
					(7, Seq(((event2, 7), NotRecorded)))
				))
			))
		))
	}}}

	@Test
	def commandApplyAttendedSome() = withFakeTime(baseLocalDateTime) { new Fixture() { withFakeTime(now) {
		val member = Fixtures.member(MemberUserType.Student, user5.getWarwickId, user5.getUserId)

		val command = new ListStudentGroupAttendanceCommandInternal(member, academicYear) with CommandTestSupport
		command.smallGroupService.findSmallGroupsByStudent(user5) returns Seq(group)
		command.smallGroupService.findSmallGroupsWithAttendanceRecorded(user5.getWarwickId) returns Seq()
		command.smallGroupService.findAttendanceByGroup(group) returns Seq(occurrence1, occurrence2, occurrence3)
		command.smallGroupService.findAttendanceNotes(
			Seq(user5).map(_.getWarwickId),
			Seq(occurrence1, occurrence3, occurrence2)
		) returns Seq()

		val autumnTerm = academicYear.termOrVacation(AcademicYearPeriod.PeriodType.autumnTerm)

		val info = command.applyInternal()
		info.missedCount should be (2)
		info.missedCountByTerm should be (Map(autumnTerm -> 2))
		info.termWeeks.toSeq should be (Seq(autumnTerm -> WeekRange(1, 10)))

		// Map all the SortedMaps to Seqs to preserve the order they've been set as
		val attendanceSeqs = info.attendance.toSeq.map { case (t, attendance) =>
			t -> attendance.toSeq.map { case (g, att) =>
				g -> att.toSeq.map { case (weekNumber, at) =>
					weekNumber -> at.toSeq
				}
			}
		}

		attendanceSeqs should be (Seq(
			(autumnTerm, Seq(
				(group, Seq(
					(1, Seq(((event2, 1), Attended))),
					(2, Seq(((event1, 2), NotExpected))),
					(3, Seq(((event1, 3), MissedUnauthorised), ((event2, 3), MissedUnauthorised))),
					(4, Seq(((event1, 4), NotExpected))),
					(7, Seq(((event2, 7), NotExpected)))
				))
			))
		))
	}}}

}