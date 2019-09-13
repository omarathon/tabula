package uk.ac.warwick.tabula.services.attendancemonitoring

import org.joda.time.{DateTime, LocalDate}
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.MemberOrUser
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, SmallGroupEventAttendance, SmallGroupEventAttendanceNote, SmallGroupEventOccurrence}
import uk.ac.warwick.tabula.data.model.{Module, StudentMember}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.collection.JavaConverters._

trait AttendanceMonitoringEventAttendanceServiceComponent {
  def attendanceMonitoringEventAttendanceService: AttendanceMonitoringEventAttendanceService
}

trait AutowiringAttendanceMonitoringEventAttendanceServiceComponent extends AttendanceMonitoringEventAttendanceServiceComponent {
  val attendanceMonitoringEventAttendanceService: AttendanceMonitoringEventAttendanceService = Wire[AttendanceMonitoringEventAttendanceService]
}

trait AttendanceMonitoringEventAttendanceService {
  def getCheckpoints(attendances: Seq[SmallGroupEventAttendance], onlyRecordable: Boolean = true): Seq[AttendanceMonitoringCheckpoint]

  def updateCheckpoints(attendances: Seq[SmallGroupEventAttendance]): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])

  def getMissedCheckpoints(attendances: Seq[SmallGroupEventAttendance], onlyRecordable: Boolean = true): Seq[(AttendanceMonitoringCheckpoint, Seq[SmallGroupEventAttendanceNote])]

  def updateMissedCheckpoints(attendances: Seq[SmallGroupEventAttendance], user: CurrentUser): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])

  def summariseSmallGroupEventAttendanceNotes(notes: Seq[SmallGroupEventAttendanceNote]): Option[String]
}

abstract class AbstractAttendanceMonitoringEventAttendanceService extends AttendanceMonitoringEventAttendanceService {

  self: ProfileServiceComponent with AttendanceMonitoringServiceComponent with SmallGroupServiceComponent =>

  def getCheckpoints(attendances: Seq[SmallGroupEventAttendance], onlyRecordable: Boolean = true): Seq[AttendanceMonitoringCheckpoint] = {
    attendances
      .filter(a => a.attended && a.event.scheduled)
      .flatMap { attendance =>
        profileService.getMemberByUniversityId(attendance.universityId).collect {
          case studentMember: StudentMember =>
            val attendedPoints = findPointsForEventAttendance(attendance, studentMember, onlyRecordable)
              .filter(point => studentAttendedEnoughOccurrences(point, attendance, studentMember))

            attendedPoints.map(point => createAttendanceCheckpoint(attendance, studentMember, point))
        }
      }.flatten
  }

  def getMissedCheckpoints(attendances: Seq[SmallGroupEventAttendance], onlyRecordable: Boolean = true): Seq[(AttendanceMonitoringCheckpoint, Seq[SmallGroupEventAttendanceNote])] = {
    attendances
      .filter(_.module.adminDepartment.autoMarkMissedMonitoringPoints)
      .filter(a => a.missed && a.event.scheduled)
      .flatMap { attendance =>
        profileService.getMemberByUniversityId(attendance.universityId).collect {
          case studentMember: StudentMember =>
            val relevantPoints = getRelevantPoints(
              attendanceMonitoringService.listStudentsPoints(studentMember, None, attendance.academicYear),
              attendance,
              studentMember,
              onlyRecordable
            )

            val missedPoints = relevantPoints.filter(point => studentCannotAttendEnoughOccurrences(point, attendance, studentMember))

            missedPoints.map { point =>
              val checkpoint = createAttendanceCheckpoint(attendance, studentMember, point)
              val eventNotes = getSmallGroupAttendanceNotesBetweenMonitoringPointDates(point, attendance.academicYear, studentMember)

              (checkpoint, eventNotes)
            }
        }
      }.flatten
  }

  def updateCheckpoints(attendances: Seq[SmallGroupEventAttendance]): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal]) = mergeCheckpointTotals {
    getCheckpoints(attendances).map(setAttendance)
  }

  def updateMissedCheckpoints(attendances: Seq[SmallGroupEventAttendance], user: CurrentUser): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal]) = mergeCheckpointTotals {
    getMissedCheckpoints(attendances).map { case (checkpoint, eventNotes) =>
      appendEventNotesToAttendanceNote(checkpoint, eventNotes, user)

      setAttendance(checkpoint)
    }
  }

  private def appendEventNotesToAttendanceNote(checkpoint: AttendanceMonitoringCheckpoint, eventNotes: Seq[SmallGroupEventAttendanceNote], user: CurrentUser) = {
    if (eventNotes.nonEmpty) {
      val newNoteSummary = summariseSmallGroupEventAttendanceNotes(eventNotes).getOrElse("")

      attendanceMonitoringService.appendToAttendanceNote(checkpoint.student, checkpoint.point, newNoteSummary, user.apparentUser)
    }
  }

  private def findPointsForEventAttendance(attendance: SmallGroupEventAttendance, studentMember: StudentMember, onlyRecordable: Boolean) = {
    attendance.date.map(eventDate =>
      getRelevantPoints(
        attendanceMonitoringService.listStudentsPointsForDate(studentMember, None, eventDate),
        attendance,
        studentMember,
        onlyRecordable
      )
    ).getOrElse(Nil)
  }

  private def getRelevantPoints(points: Seq[AttendanceMonitoringPoint], attendance: SmallGroupEventAttendance, studentMember: StudentMember, onlyRecordable: Boolean): Seq[AttendanceMonitoringPoint] = {
    attendance.date.map(eventDate =>
      points.filter(point =>
        // Is it the correct type
        point.pointType == AttendanceMonitoringPointType.SmallGroup
          // Is the attendance inside the point's weeks
          && point.containsDate(eventDate)
          // Is the group's module valid
          && (point.smallGroupEventModules.isEmpty || point.smallGroupEventModules.contains(attendance.module))
          && (!onlyRecordable || (
          // Is there no existing checkpoint
          noExistingCheckpoint(point, attendance, studentMember)
            // The student hasn't been sent to SITS for this point
            && !attendanceMonitoringService.studentAlreadyReportedThisTerm(studentMember, point)))
      )).getOrElse(Nil)
  }

  // false if there is an existing manual checkpoint or automatic checkpoint with the same state
  private def noExistingCheckpoint(point: AttendanceMonitoringPoint, attendance: SmallGroupEventAttendance, studentMember: StudentMember): Boolean = {
    val checkpoint = attendanceMonitoringService.getCheckpoints(Seq(point), Seq(studentMember)).values.headOption.flatMap(_.values.headOption)
    !checkpoint.exists(c => !c.autoCreated || c.state == attendance.state)
  }

  private def studentAttendedEnoughOccurrences(point: AttendanceMonitoringPoint, attendance: SmallGroupEventAttendance, student: StudentMember): Boolean = {
    if (point.smallGroupEventQuantity == 1) {
      true
    } else {
      val attendances = findAttendanceForPoint(point, attendance.academicYear, student)
        .filterNot(a => a.occurrence == attendance.occurrence)
        .filter(_.state == AttendanceState.Attended)
      point.smallGroupEventQuantity <= attendances.size + 1
    }
  }

  private def createAttendanceCheckpoint(attendance: SmallGroupEventAttendance, studentMember: StudentMember, point: AttendanceMonitoringPoint) = {
    val checkpoint = new AttendanceMonitoringCheckpoint
    checkpoint.autoCreated = true
    checkpoint.point = point
    checkpoint.attendanceMonitoringService = attendanceMonitoringService
    checkpoint.student = studentMember
    checkpoint.updatedBy = attendance.updatedBy
    checkpoint.updatedDate = DateTime.now
    checkpoint.state = attendance.state
    checkpoint
  }

  private def findEventOccurrencesForDates(point: AttendanceMonitoringPoint, academicYear: AcademicYear, studentMember: StudentMember): Seq[SmallGroupEventOccurrence] = {
    findEventOccurrencesForDates(
      startDate = point.startDate,
      endDate = point.endDate,
      academicYear = academicYear,
      student = studentMember,
      modules = point.smallGroupEventModules
    )
  }

  private def studentCannotAttendEnoughOccurrences(point: AttendanceMonitoringPoint, attendance: SmallGroupEventAttendance, student: StudentMember): Boolean = {
    // find possible events that this student can go to that have not been already marked
    // find SmallGroupEventAttendance marked attended already for AttendanceMonitoringPoint
    val attendedEvents = findAttendanceForPoint(point, attendance.academicYear, student).filter(_.attended)
    val unmarkedEventOccurrences = findEventOccurrencesForDates(point, attendance.academicYear, student)
      .filter(occurrence => occurrence != attendance.occurrence && !isEventAttendanceMarkedForStudent(occurrence, student))
    (attendedEvents.size + unmarkedEventOccurrences.size) < point.smallGroupEventQuantity
  }

  private def setAttendance(checkpoint: AttendanceMonitoringCheckpoint) = {
    attendanceMonitoringService.setAttendance(checkpoint.student, Map(checkpoint.point -> checkpoint.state), checkpoint.updatedBy, autocreated = true)
  }

  private def getSmallGroupAttendanceNotesBetweenMonitoringPointDates(point: AttendanceMonitoringPoint, academicYear: AcademicYear, studentMember: StudentMember): Seq[SmallGroupEventAttendanceNote] = {
    val occurrences = findEventOccurrencesForDates(point, academicYear, studentMember)

    smallGroupService.findAttendanceNotes(Seq(studentMember.universityId), occurrences)
  }

  def summariseSmallGroupEventAttendanceNotes(notes: Seq[SmallGroupEventAttendanceNote]): Option[String] = {
    Option("Summary of small group event attendance notes: " + notes.map(note => s"[Occurrence ID: ${note.occurrence.id}, Event: ${note.occurrence.event.title}, Details: ${note.note}]").mkString(", ")).filter(_.nonEmpty)
  }

  private def mergeCheckpointTotals(input: Seq[(Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])]): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal]) = {
    input.foldLeft((Seq[AttendanceMonitoringCheckpoint](), Seq[AttendanceMonitoringCheckpointTotal]())) {
      case ((leftCheckpoints, leftTotals), (rightCheckpoints, rightTotals)) => (leftCheckpoints ++ rightCheckpoints, leftTotals ++ rightTotals)
    }
  }

  private def findAttendanceForPoint(point: AttendanceMonitoringPoint, academicYear: AcademicYear, student: StudentMember) = {
    findAttendanceForDates(point.startDate, point.endDate, academicYear, student, point.smallGroupEventModules)
  }

  private def isEventAttendanceMarkedForStudent(occurrence: SmallGroupEventOccurrence, student: StudentMember): Boolean = {
    occurrence.attendance.asScala.exists(_.universityId == student.universityId)
  }

  private def findAttendanceForDates(
    startDate: LocalDate,
    endDate: LocalDate,
    academicYear: AcademicYear,
    student: StudentMember,
    modules: Seq[Module]
  ): Seq[SmallGroupEventAttendance] = {
    val startWeek = academicYear.weekForDate(startDate).weekNumber
    val endWeek = academicYear.weekForDate(endDate).weekNumber

    val weekAttendances = smallGroupService.findAttendanceForStudentInModulesInWeeks(student, startWeek, endWeek, academicYear, modules)

    val byDate = filterOccurrencesByDate(academicYear, startDate, endDate)

    weekAttendances.filter(attendance => byDate(attendance.occurrence))
  }

  private def findEventOccurrencesForDates(
    startDate: LocalDate,
    endDate: LocalDate,
    academicYear: AcademicYear,
    student: StudentMember,
    modules: Seq[Module]
  ): Seq[SmallGroupEventOccurrence] = {
    val startWeek = academicYear.weekForDate(startDate).weekNumber
    val endWeek = academicYear.weekForDate(endDate).weekNumber

    val weekOccurrences = (modules match {
      case Nil => smallGroupService.findOccurrencesInWeeks(startWeek, endWeek, academicYear)
      case _ => smallGroupService.findOccurrencesInModulesInWeeks(startWeek, endWeek, modules, academicYear)
    }).filter(_.event.group.groupSet.showAttendanceReports).filter(_.event.group.students.includesUser(MemberOrUser(student).asUser))

    val byDate = filterOccurrencesByDate(academicYear, startDate, endDate)

    weekOccurrences.filter(byDate)
  }

  private def filterOccurrencesByDate(academicYear: AcademicYear, startDate: LocalDate, endDate: LocalDate): SmallGroupEventOccurrence => Boolean = {
    // weekAttendances may contain attendance before the startDate and after the endDate, so filter those out
    // don't need to filter if the startDate is a Monday and the endDate is a Sunday, as that's the whole week

    val startWeek = academicYear.weekForDate(startDate).weekNumber
    val endWeek = academicYear.weekForDate(endDate).weekNumber

    if (startDate.getDayOfWeek == DayOfWeek.Monday.jodaDayOfWeek && endDate.getDayOfWeek == DayOfWeek.Sunday.jodaDayOfWeek) {
      _: SmallGroupEventOccurrence => true
    } else {
      a: SmallGroupEventOccurrence => {
        a.week > startWeek && a.week < endWeek ||
          a.week == startWeek && a.event.day.jodaDayOfWeek >= startDate.getDayOfWeek ||
          a.week == endWeek && a.event.day.jodaDayOfWeek <= endDate.getDayOfWeek
      }
    }
  }

}

@Service("attendanceMonitoringEventAttendanceService")
class AttendanceMonitoringEventAttendanceServiceImpl
  extends AbstractAttendanceMonitoringEventAttendanceService
    with AutowiringAttendanceMonitoringServiceComponent
    with AutowiringProfileServiceComponent
    with AutowiringSmallGroupServiceComponent
