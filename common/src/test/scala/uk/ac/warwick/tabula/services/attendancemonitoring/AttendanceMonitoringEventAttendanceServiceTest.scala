package uk.ac.warwick.tabula.services.attendancemonitoring

import org.joda.time.DateTimeConstants
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState.Attended
import uk.ac.warwick.tabula.data.model.{Module, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class AttendanceMonitoringEventAttendanceServiceTest extends TestBase with Mockito {

  val mockModuleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]

  trait ServiceTestSupport extends SmallGroupServiceComponent
    with ProfileServiceComponent with AttendanceMonitoringServiceComponent {

    val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
    val profileService: ProfileService = smartMock[ProfileService]
    val smallGroupService: SmallGroupService = smartMock[SmallGroupService]
  }

  trait Fixture {
    val service = new AbstractAttendanceMonitoringEventAttendanceService with ServiceTestSupport

    val academicYear2013 = AcademicYear(2013)

    val student: StudentMember = Fixtures.student("1234")

    val group = new SmallGroup
    val groupSet = new SmallGroupSet
    groupSet.academicYear = academicYear2013
    group.groupSet = groupSet

    val event = new SmallGroupEvent(group)
    event.day = DayOfWeek.Wednesday

    val occurrence = new SmallGroupEventOccurrence
    occurrence.event = event
    occurrence.week = 1

    val attendance = new SmallGroupEventAttendance
    attendance.occurrence = occurrence
    attendance.universityId = student.universityId
    attendance.state = AttendanceState.Attended
    occurrence.attendance.add(attendance)
    attendance.updatedBy = "cusfal"

    service.profileService.getMemberByUniversityId(student.universityId) returns Option(student)

    val module1: Module = Fixtures.module("aa101")
    module1.id = "aa101"
    val module2: Module = Fixtures.module("aa202")
    module2.id = "aa202"
    mockModuleAndDepartmentService.getModuleById(module1.id) returns Option(module1)
    mockModuleAndDepartmentService.getModuleById(module2.id) returns Option(module2)

    val smallGroupPoint = new AttendanceMonitoringPoint
    // start date: Tuesday week 1
    smallGroupPoint.startDate = academicYear2013.weeks(1).firstDay.withDayOfWeek(DateTimeConstants.TUESDAY)
    // end date: Thursday week 2
    smallGroupPoint.endDate = academicYear2013.weeks(2).firstDay.withDayOfWeek(DateTimeConstants.THURSDAY)
    smallGroupPoint.pointType = AttendanceMonitoringPointType.SmallGroup
    smallGroupPoint.smallGroupEventModules = Seq()
    smallGroupPoint.smallGroupEventQuantity = 1
    smallGroupPoint.moduleAndDepartmentService = mockModuleAndDepartmentService

    service.attendanceMonitoringService.listStudentsPointsForDate(student, None, occurrence.date.get) returns Seq(smallGroupPoint)


    service.attendanceMonitoringService.getCheckpoints(Seq(smallGroupPoint), Seq(student)) returns Map()
    service.attendanceMonitoringService.studentAlreadyReportedThisTerm(student, smallGroupPoint) returns false
    service.attendanceMonitoringService.setAttendance(student, Map(smallGroupPoint -> AttendanceState.Attended), attendance.updatedBy, autocreated = true) returns
      ((Seq(Fixtures.attendanceMonitoringCheckpoint(smallGroupPoint, student, AttendanceState.Attended)), Seq[AttendanceMonitoringCheckpointTotal]()))

  }


  @Test
  def updatesCheckpoint() {
    new Fixture {
      service.getCheckpoints(Seq(attendance)).size should be(1)
      service.updateCheckpoints(Seq(attendance))
      verify(service.attendanceMonitoringService, times(1)).setAttendance(student, Map(smallGroupPoint -> AttendanceState.Attended), attendance.updatedBy, autocreated = true)
    }
  }

  @Test
  def updatesCheckpointQuantityMoreThanOne() {
    new Fixture {
      val otherAttendance = new SmallGroupEventAttendance
      otherAttendance.occurrence = new SmallGroupEventOccurrence
      otherAttendance.occurrence.week = 2
      otherAttendance.occurrence.event = new SmallGroupEvent
      otherAttendance.occurrence.event.day = DayOfWeek.Monday
      otherAttendance.universityId = student.universityId
      otherAttendance.state = Attended

      service.smallGroupService.findAttendanceForStudentInModulesInWeeks(student, 1, 2, academicYear2013, smallGroupPoint.smallGroupEventModules) returns Seq(otherAttendance)

      smallGroupPoint.smallGroupEventQuantity = 2

      service.getCheckpoints(Seq(attendance)).size should be(1)

      service.updateCheckpoints(Seq(attendance))
      verify(service.attendanceMonitoringService, times(1)).setAttendance(student, Map(smallGroupPoint -> AttendanceState.Attended), attendance.updatedBy, autocreated = true)
    }
  }

  @Test
  def updatesCheckpointSpecificModule() {
    new Fixture {
      smallGroupPoint.smallGroupEventModules = Seq(module1)
      groupSet.module = module1

      service.getCheckpoints(Seq(attendance)).size should be(1)

      service.updateCheckpoints(Seq(attendance))
      verify(service.attendanceMonitoringService, times(1)).setAttendance(student, Map(smallGroupPoint -> AttendanceState.Attended), attendance.updatedBy, autocreated = true)
    }
  }

  @Test
  def notAttended() {
    new Fixture {
      attendance.state = AttendanceState.MissedAuthorised
      service.getCheckpoints(Seq(attendance)).size should be(0)
    }
  }

  @Test
  def notAMember() {
    new Fixture {
      val nonMember = "notamember"
      attendance.universityId = nonMember
      service.profileService.getMemberByUniversityId(nonMember) returns None
      service.getCheckpoints(Seq(attendance)).size should be(0)
    }
  }

  @Test
  def noPoints() {
    new Fixture {
      service.attendanceMonitoringService.listStudentsPointsForDate(student, None, occurrence.date.get) returns Seq()
      service.getCheckpoints(Seq(attendance)).size should be(0)
    }
  }

  @Test
  def wrongPointType() {
    new Fixture {
      smallGroupPoint.pointType = AttendanceMonitoringPointType.Meeting
      service.getCheckpoints(Seq(attendance)).size should be(0)
    }
  }

  @Test
  def wrongWeek() {
    new Fixture {
      occurrence.week = 3
      service.attendanceMonitoringService.listStudentsPointsForDate(student, None, occurrence.date.get) returns Seq(smallGroupPoint)
      service.getCheckpoints(Seq(attendance)).size should be(0)
    }
  }


  @Test
  def wrongModule() {
    new Fixture {
      smallGroupPoint.smallGroupEventModules = Seq(module1)
      groupSet.module = module2
      service.getCheckpoints(Seq(attendance)).size should be(0)
    }
  }

  @Test
  def checkpointAlreadyExists() {
    new Fixture {
      service.attendanceMonitoringService.getCheckpoints(Seq(smallGroupPoint), Seq(student)) returns
        Map(
          student -> Map(
            smallGroupPoint -> Fixtures.attendanceMonitoringCheckpoint(smallGroupPoint, student, AttendanceState.Attended)
          )
        )
      service.getCheckpoints(Seq(attendance)).size should be(0)

    }
  }

  @Test
  def reportedToSITS() {
    new Fixture {
      service.attendanceMonitoringService.studentAlreadyReportedThisTerm(student, smallGroupPoint) returns true
      service.getCheckpoints(Seq(attendance)).size should be(0)
    }
  }

  @Test
  def notEnoughAttendanceCurrentNotPersisted() {
    new Fixture {
      smallGroupPoint.smallGroupEventQuantity = 2
      service.smallGroupService.findAttendanceForStudentInModulesInWeeks(student, 1, 2, academicYear2013, smallGroupPoint.smallGroupEventModules) returns Seq()
      service.getCheckpoints(Seq(attendance)).size should be(0)
    }
  }

  @Test
  def notEnoughAttendanceCurrentPersisted() {
    new Fixture {
      smallGroupPoint.smallGroupEventQuantity = 2
      service.smallGroupService.findAttendanceForStudentInModulesInWeeks(student, 1, 2, academicYear2013, smallGroupPoint.smallGroupEventModules) returns Seq(attendance)
      service.getCheckpoints(Seq(attendance)).size should be(0)
    }
  }

  @Test
  def notEnoughAttendanceBeforeStart() {
    new Fixture {
      // Start week on Monday before start date (Tuesday)
      val otherAttendance = new SmallGroupEventAttendance
      otherAttendance.occurrence = new SmallGroupEventOccurrence
      otherAttendance.occurrence.week = 1
      otherAttendance.occurrence.event = new SmallGroupEvent
      otherAttendance.occurrence.event.day = DayOfWeek.Monday
      otherAttendance.universityId = student.universityId

      service.smallGroupService.findAttendanceForStudentInModulesInWeeks(student, 1, 2, academicYear2013, smallGroupPoint.smallGroupEventModules) returns Seq(otherAttendance)

      smallGroupPoint.smallGroupEventQuantity = 2

      service.getCheckpoints(Seq(attendance)).size should be(0)
    }
  }

  @Test
  def notEnoughAttendanceAfterEnd() {
    new Fixture {
      // End week on Friday after end date (Thursday)
      val otherAttendance = new SmallGroupEventAttendance
      otherAttendance.occurrence = new SmallGroupEventOccurrence
      otherAttendance.occurrence.week = 2
      otherAttendance.occurrence.event = new SmallGroupEvent
      otherAttendance.occurrence.event.day = DayOfWeek.Friday
      otherAttendance.universityId = student.universityId

      service.smallGroupService.findAttendanceForStudentInModulesInWeeks(student, 1, 2, academicYear2013, smallGroupPoint.smallGroupEventModules) returns Seq(otherAttendance)

      smallGroupPoint.smallGroupEventQuantity = 2

      service.getCheckpoints(Seq(attendance)).size should be(0)
    }
  }


}
