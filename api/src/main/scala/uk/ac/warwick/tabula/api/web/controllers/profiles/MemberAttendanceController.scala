package uk.ac.warwick.tabula.api.web.controllers.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.FileAttachmentToJsonConverter
import uk.ac.warwick.tabula.commands.attendance.profile.{AttendanceProfileCommand, AttendanceProfileCommandResult}
import uk.ac.warwick.tabula.commands.groups.{ListStudentGroupAttendanceCommand, StudentGroupAttendance}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringPointStyle, AttendanceState}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.{AcademicYear, DateFormats, ItemNotFoundException, PermissionDeniedException}

import scala.language.implicitConversions

@Controller
@RequestMapping(Array("/v1/member/{member}/attendance"))
class MemberAttendanceController extends ApiController
	with GetMemberAttendanceApi
	with MonitoringPointsResultToJsonConverter
	with StudentGroupAttendanceToJsonConverter
	with FileAttachmentToJsonConverter {

	@ModelAttribute("student")
	def student(@PathVariable member: Member): StudentMember =
		mandatory(member) match {
			case student: StudentMember => student
			case m => throw new ItemNotFoundException(s"${m.universityId} is not a student")
		}

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def getAttendanceForCurrentYear(@ModelAttribute("student") student: StudentMember): Mav =
		getAttendanceForYear(student, AcademicYear.now())

	@RequestMapping(value = Array("/{academicYear}"), method = Array(GET), produces = Array("application/json"))
	def getAttendanceForYear(@ModelAttribute("student") student: StudentMember, @PathVariable academicYear: AcademicYear): Mav =
		attendanceForYearAsJson(student, academicYear)

}

@Controller
@RequestMapping(Array("/v1/member/{member}/course/{studentCourseDetails}/{academicYear}/attendance"))
class MemberAttendanceForCourseYearController extends ApiController
	with GetMemberAttendanceApi
	with MonitoringPointsResultToJsonConverter
	with StudentGroupAttendanceToJsonConverter
	with FileAttachmentToJsonConverter {

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def getAttendanceForCourseYear(@PathVariable member: Member, @PathVariable studentCourseDetails: StudentCourseDetails, @PathVariable academicYear: AcademicYear): Mav = {
		mustBeLinked(studentCourseDetails, member)
		attendanceForYearAsJson(mandatory(studentCourseDetails.student), mandatory(academicYear))
	}

}

trait MonitoringPointsResultToJsonConverter {
	self: FileAttachmentToJsonConverter =>

	implicit def orderedAttendanceMonitoringPoint(p: AttendanceMonitoringPoint): math.Ordered[AttendanceMonitoringPoint] =
		(p2: AttendanceMonitoringPoint) => {
			if (p.scheme.pointStyle != p2.scheme.pointStyle) throw new IllegalArgumentException

			p.scheme.pointStyle match {
				case AttendanceMonitoringPointStyle.Week =>
					if (p.startWeek.compareTo(p2.startWeek) != 0)
						p.startWeek.compareTo(p2.startWeek)
					else
						p.endWeek.compareTo(p2.endWeek)

				case AttendanceMonitoringPointStyle.Date =>
					if (p.startDate.compareTo(p2.startDate) != 0)
						p.startDate.compareTo(p2.startDate)
					else
						p.endDate.compareTo(p2.endDate)
			}
		}

	def monitoringPointsResultAsJson(result: AttendanceProfileCommandResult): Seq[Map[String, Any]] =
		result.groupedPointMap.values.flatten.toSeq.sortBy { case (point, _) => point }.map { case (point, checkpoint) =>
			val pointDates = point.scheme.pointStyle match {
				case AttendanceMonitoringPointStyle.Week =>
					Map(
						"startWeek" -> point.startWeek,
						"endWeek" -> point.endWeek,
						"term" -> point.scheme.academicYear.weeks(point.startWeek).period.periodType.toString
					)

				case AttendanceMonitoringPointStyle.Date =>
					Map(
						"startDate" -> DateFormats.IsoDate.print(point.startDate),
						"endDate" -> DateFormats.IsoDate.print(point.endDate)
					)
			}

			val checkpointJson = Option(checkpoint).map { cp =>
				Map(
					"state" -> cp.state.dbValue,
					"autoCreated" -> cp.autoCreated,
					"updatedDate" -> DateFormats.IsoDateTime.print(cp.updatedDate),
					"updatedBy" -> cp.updatedBy
				)
			}.getOrElse(Map("state" -> AttendanceState.NotRecorded.dbValue))

			val noteJson =
				if (Option(checkpoint).nonEmpty)
					result.noteCheckpoints.find { case (_, cp) => cp.contains(checkpoint) }.map { case (note, _) =>
						Map(
							"note" -> (Map(
								"absenceType" -> note.absenceType.dbValue,
								"contents" -> note.note,
								"updatedDate" -> DateFormats.IsoDateTime.print(note.updatedDate),
								"updatedBy" -> note.updatedBy
							) ++ (if (Option(note.attachment).nonEmpty) Map("attachment" -> "attachment" -> jsonFileAttachmentObject(note.attachment)) else Map()))
						)
					}.getOrElse(Map())
				else Map()

			Map(
				"point" -> (Map(
					"name" -> point.name
				) ++ pointDates)
			) ++ checkpointJson ++ noteJson
		}
}

trait StudentGroupAttendanceToJsonConverter {
	self: FileAttachmentToJsonConverter =>

	def studentGroupAttendanceAsJson(result: StudentGroupAttendance): Seq[Map[String, Any]] =
		result.attendance
			.values.flatten
			.groupBy { case (group, _) => group }
  		.mapValues(_.map { case (_, a) => a })
			.toSeq
			.groupBy { case (group, _) => group.groupSet }
  		.toSeq
  		.sortBy { case (set, a) => (a.map { case (_, attendance) => attendance.map(_.keys).min }.min, set.module.code, set.name, set.id) }
			.map { case (set, groupAttendance) =>
			Map(
				"groupSet" -> Map(
					"id" -> set.id,
					"module" -> Map(
						"code" -> set.module.code.toUpperCase,
						"name" -> set.module.name,
						"adminDepartment" -> Map(
							"code" -> set.module.adminDepartment.code.toUpperCase,
							"name" -> set.module.adminDepartment.name
						)
					),
					"name" -> set.name,
					"format" -> set.format.code
				),
				"groups" -> groupAttendance.map { case (group, attendance) =>
					Map(
						"group" -> Map(
							"id" -> group.id,
							"name" -> group.name
						),
						"attendance" -> attendance.flatten.flatMap { case (_, instanceAttendance) =>
							instanceAttendance.toSeq.map { case ((event, weekNumber), state) =>
								val noteJson =
									result.notes.get((event, weekNumber)).map { note =>
										Map(
											"note" -> (Map(
												"absenceType" -> note.absenceType.dbValue,
												"contents" -> note.note,
												"updatedDate" -> DateFormats.IsoDateTime.print(note.updatedDate),
												"updatedBy" -> note.updatedBy
											) ++ (if (Option(note.attachment).nonEmpty) Map("attachment" -> "attachment" -> jsonFileAttachmentObject(note.attachment)) else Map()))
										)
									}.getOrElse(Map())

								Map(
									"event" -> Map(
										"id" -> event.id,
										"title" -> event.title,
										"day" -> Option(event.day).map { _.name }.orNull,
										"startTime" -> Option(event.startTime).map { _.toString("HH:mm") }.orNull,
										"endTime" -> Option(event.endTime).map { _.toString("HH:mm") }.orNull,
										"location" -> Option(event.location).map {
											case NamedLocation(name) => Map("name" -> name)
											case MapLocation(name, locationId, syllabusPlusName) =>
												Map("name" -> name, "locationId" -> locationId) ++ syllabusPlusName.map(n => Map("syllabusPlusName" -> n)).getOrElse(Map())
											case AliasedMapLocation(name, MapLocation(_, locationId, syllabusPlusName)) =>
												Map("name" -> name, "locationId" -> locationId) ++ syllabusPlusName.map(n => Map("syllabusPlusName" -> n)).getOrElse(Map())
										}.orNull,
										"tutors" -> event.tutors.users.map { user =>
											Seq(
												user.getUserId.maybeText.map { "userId" -> _ },
												user.getWarwickId.maybeText.map { "universityId" -> _ }
											).flatten.toMap
										}
									),
									"weekNumber" -> weekNumber,
									"state" -> state.getName
								) ++ noteJson
							}
						}
					)
				}
			)
		}
}

trait GetMemberAttendanceApi {
	self: ApiController
		with MonitoringPointsResultToJsonConverter
		with StudentGroupAttendanceToJsonConverter
		with FileAttachmentToJsonConverter =>

	def attendanceForYearAsJson(student: StudentMember, academicYear: AcademicYear): Mav = {
		val monitoringPointAttendanceCommand = restricted(AttendanceProfileCommand(mandatory(student), mandatory(academicYear)))
		val seminarAttendanceCommand = restricted(ListStudentGroupAttendanceCommand(mandatory(student), mandatory(academicYear)))

		if (monitoringPointAttendanceCommand.isEmpty && seminarAttendanceCommand.isEmpty)
			throw new PermissionDeniedException(user, Seq(Permissions.MonitoringPoints.View, Permissions.Profiles.Read.SmallGroups, Permissions.SmallGroupEvents.ViewRegister), student)

		val monitoringPointsJson: Map[String, Any] =
			monitoringPointAttendanceCommand.map { cmd =>
				Map(
					"monitoringPoints" -> monitoringPointsResultAsJson(cmd.apply())
				)
			}.getOrElse(Map.empty[String, Any])

		val smallGroupsJson: Map[String, Any] =
			seminarAttendanceCommand.map { cmd =>
				Map(
					"smallGroups" -> studentGroupAttendanceAsJson(cmd.apply())
				)
			}.getOrElse(Map.empty[String, Any])

		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok"
		) ++ monitoringPointsJson ++ smallGroupsJson))
	}
}
