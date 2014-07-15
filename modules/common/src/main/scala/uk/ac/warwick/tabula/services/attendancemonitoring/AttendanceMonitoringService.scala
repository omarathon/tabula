package uk.ac.warwick.tabula.services.attendancemonitoring

import org.codehaus.jackson.annotate.JsonAutoDetect
import org.joda.time.DateTime
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.convert.{DepartmentCodeConverter, MemberUniversityIdConverter}
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.data.{AttendanceMonitoringDaoComponent, AttendanceMonitoringStudentData, AutowiringAttendanceMonitoringDaoComponent, Daoisms, SchemeMembershipItem, SchemeMembershipItemType}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.queue.conversion.ItemType
import uk.ac.warwick.util.queue.{Queue, QueueListener}

import scala.beans.BeanProperty
import scala.collection.JavaConverters._

trait AttendanceMonitoringServiceComponent {
	def attendanceMonitoringService: AttendanceMonitoringService
}

trait AutowiringAttendanceMonitoringServiceComponent extends AttendanceMonitoringServiceComponent {
	val attendanceMonitoringService = Wire[AttendanceMonitoringService]
}

trait AttendanceMonitoringService {
	def getSchemeById(id: String): Option[AttendanceMonitoringScheme]
	def getPointById(id: String): Option[AttendanceMonitoringPoint]
	def saveOrUpdate(scheme: AttendanceMonitoringScheme): Unit
	def saveOrUpdate(point: AttendanceMonitoringPoint): Unit
	def saveOrUpdate(template: AttendanceMonitoringTemplate): Unit
	def saveOrUpdate(templatePoint: AttendanceMonitoringTemplatePoint): Unit
	def saveOrUpdate(note: AttendanceMonitoringNote): Unit
	def saveOrUpdate(report: MonitoringPointReport): Unit
	def deleteScheme(scheme: AttendanceMonitoringScheme)
	def deletePoint(point: AttendanceMonitoringPoint)
	def deleteTemplate(template: AttendanceMonitoringTemplate)
	def deleteTemplatePoint(point: AttendanceMonitoringTemplatePoint)
	def getTemplateSchemeById(id: String): Option[AttendanceMonitoringTemplate]
	def getTemplatePointById(id: String): Option[AttendanceMonitoringTemplatePoint]
	def listAllSchemes(department: Department): Seq[AttendanceMonitoringScheme]
	def listSchemes(department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringScheme]
	def listAllTemplateSchemes: Seq[AttendanceMonitoringTemplate]
	def listTemplateSchemesByStyle(style: AttendanceMonitoringPointStyle): Seq[AttendanceMonitoringTemplate]
	def listOldSets(department: Department, academicYear: AcademicYear): Seq[MonitoringPointSet]
	def listSchemesForMembershipUpdate: Seq[AttendanceMonitoringScheme]
	def findNonReportedTerms(students: Seq[StudentMember], academicYear: AcademicYear): Seq[String]
	def studentAlreadyReportedThisTerm(student: StudentMember, point: AttendanceMonitoringPoint): Boolean
	def findReports(studentIds: Seq[String], year: AcademicYear, period: String): Seq[MonitoringPointReport]
	def findSchemeMembershipItems(universityIds: Seq[String], itemType: SchemeMembershipItemType): Seq[SchemeMembershipItem]
	def findPoints(
		department: Department,
		academicYear: AcademicYear,
		schemes: Seq[AttendanceMonitoringScheme],
		types: Seq[AttendanceMonitoringPointType],
		styles: Seq[AttendanceMonitoringPointStyle]
	): Seq[AttendanceMonitoringPoint]
	def findOldPoints(
		department: Department,
		academicYear: AcademicYear,
		sets: Seq[MonitoringPointSet],
		types: Seq[AttendanceMonitoringPointType]
	): Seq[MonitoringPoint]
	def listStudentsPoints(student: StudentMember, departmentOption: Option[Department], academicYear: AcademicYear): Seq[AttendanceMonitoringPoint]
	def listStudentsPoints(studentData: AttendanceMonitoringStudentData, department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringPoint]
	def getAllCheckpoints(point: AttendanceMonitoringPoint): Seq[AttendanceMonitoringCheckpoint]
	def getCheckpoints(points: Seq[AttendanceMonitoringPoint], student: StudentMember, withFlush: Boolean = false): Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint]
	def getCheckpoints(points: Seq[AttendanceMonitoringPoint], students: Seq[StudentMember]): Map[StudentMember, Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint]]
	def countCheckpointsForPoint(point: AttendanceMonitoringPoint): Int
	def getAttendanceNote(student: StudentMember, point: AttendanceMonitoringPoint): Option[AttendanceMonitoringNote]
	def getAttendanceNoteMap(student: StudentMember): Map[AttendanceMonitoringPoint, AttendanceMonitoringNote]
	def setAttendance(student: StudentMember, attendanceMap: Map[AttendanceMonitoringPoint, AttendanceState], user: CurrentUser): Seq[AttendanceMonitoringCheckpoint]
	def setAttendance(student: StudentMember, attendanceMap: Map[AttendanceMonitoringPoint, AttendanceState], usercode: String, autocreated: Boolean = false): Seq[AttendanceMonitoringCheckpoint]
	def updateCheckpointTotalsAsync(students: Seq[StudentMember], department: Department, academicYear: AcademicYear): Unit
	def updateCheckpointTotal(student: StudentMember, department: Department, academicYear: AcademicYear): AttendanceMonitoringCheckpointTotal
	def getCheckpointTotal(student: StudentMember, departmentOption: Option[Department], academicYear: AcademicYear): AttendanceMonitoringCheckpointTotal
	def getCheckpointTotals(students: Seq[StudentMember], department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringCheckpointTotal]
	def generatePointsFromTemplateScheme(templateScheme: AttendanceMonitoringTemplate, academicYear: AcademicYear): Seq[AttendanceMonitoringPoint]

}

abstract class AbstractAttendanceMonitoringService extends AttendanceMonitoringService with TaskBenchmarking {

	self: AttendanceMonitoringDaoComponent with TermServiceComponent with AttendanceMonitoringMembershipHelpers with UserLookupComponent =>

	var queue = Wire.named[Queue]("settingsSyncTopic")

	def getSchemeById(id: String): Option[AttendanceMonitoringScheme] =
		attendanceMonitoringDao.getSchemeById(id)

	def getPointById(id: String): Option[AttendanceMonitoringPoint] =
		attendanceMonitoringDao.getPointById(id)

	def saveOrUpdate(scheme: AttendanceMonitoringScheme): Unit =
		attendanceMonitoringDao.saveOrUpdate(scheme)

	def saveOrUpdate(point: AttendanceMonitoringPoint): Unit =
		attendanceMonitoringDao.saveOrUpdate(point)

	def saveOrUpdate(template: AttendanceMonitoringTemplate): Unit =
		attendanceMonitoringDao.saveOrUpdate(template)

	def saveOrUpdate(templatePoint: AttendanceMonitoringTemplatePoint): Unit =
		attendanceMonitoringDao.saveOrUpdate(templatePoint)

	def saveOrUpdate(note: AttendanceMonitoringNote): Unit =
		attendanceMonitoringDao.saveOrUpdate(note)

	def saveOrUpdate(report: MonitoringPointReport): Unit =
		attendanceMonitoringDao.saveOrUpdate(report)

	def deleteScheme(scheme: AttendanceMonitoringScheme) =
		attendanceMonitoringDao.delete(scheme)

	def deletePoint(point: AttendanceMonitoringPoint) =
		attendanceMonitoringDao.delete(point)

	def deleteTemplate(template: AttendanceMonitoringTemplate) =
		attendanceMonitoringDao.delete(template)

	def deleteTemplatePoint(point: AttendanceMonitoringTemplatePoint) =
		attendanceMonitoringDao.delete(point)

	def getTemplateSchemeById(id: String): Option[AttendanceMonitoringTemplate] =
		attendanceMonitoringDao.getTemplateSchemeById(id)

	def getTemplatePointById(id: String): Option[AttendanceMonitoringTemplatePoint] =
		attendanceMonitoringDao.getTemplatePointById(id)
	def listAllSchemes(department: Department): Seq[AttendanceMonitoringScheme] =
		attendanceMonitoringDao.listAllSchemes(department)

	def listSchemes(department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringScheme] =
		attendanceMonitoringDao.listSchemes(department, academicYear)

	def listOldSets(department: Department, academicYear: AcademicYear): Seq[MonitoringPointSet] =
		attendanceMonitoringDao.listOldSets(department, academicYear)

	def listSchemesForMembershipUpdate: Seq[AttendanceMonitoringScheme] =
		attendanceMonitoringDao.listSchemesForMembershipUpdate

	def findNonReportedTerms(students: Seq[StudentMember], academicYear: AcademicYear): Seq[String] =
		attendanceMonitoringDao.findNonReportedTerms(students, academicYear)

	def studentAlreadyReportedThisTerm(student: StudentMember, point: AttendanceMonitoringPoint): Boolean =
		!findNonReportedTerms(Seq(student), point.scheme.academicYear).contains(
			termService.getTermFromDateIncludingVacations(point.startDate.toDateTimeAtStartOfDay).getTermTypeAsString
		)

	def findReports(studentIds: Seq[String], year: AcademicYear, period: String): Seq[MonitoringPointReport] =
		attendanceMonitoringDao.findReports(studentIds, year, period)

	def findSchemeMembershipItems(universityIds: Seq[String], itemType: SchemeMembershipItemType): Seq[SchemeMembershipItem] = {
		val items = attendanceMonitoringDao.findSchemeMembershipItems(universityIds, itemType)
		items.map{ item => {
			val user = userLookup.getUserByWarwickUniId(item.universityId)
			SchemeMembershipItem(
				item.itemType,
				item.firstName,
				item.lastName,
				item.universityId,
				item.userId,
				membersHelper.findBy(user)
			)
		}}
	}

	def findPoints(
		department: Department,
		academicYear: AcademicYear,
		schemes: Seq[AttendanceMonitoringScheme],
		types: Seq[AttendanceMonitoringPointType],
		styles: Seq[AttendanceMonitoringPointStyle]
	): Seq[AttendanceMonitoringPoint] = {
		attendanceMonitoringDao.findPoints(department, academicYear, schemes, types, styles)
	}

	def findOldPoints(
		department: Department,
		academicYear: AcademicYear,
		sets: Seq[MonitoringPointSet],
		types: Seq[AttendanceMonitoringPointType]
	): Seq[MonitoringPoint] = {
		attendanceMonitoringDao.findOldPoints(department, academicYear, sets, types.map {
			case AttendanceMonitoringPointType.Standard => null
			case AttendanceMonitoringPointType.Meeting => MonitoringPointType.Meeting
			case AttendanceMonitoringPointType.SmallGroup => MonitoringPointType.SmallGroup
			case AttendanceMonitoringPointType.AssignmentSubmission => MonitoringPointType.AssignmentSubmission
		})
	}

	def listAllTemplateSchemes: Seq[AttendanceMonitoringTemplate] = {
		attendanceMonitoringDao.listAllTemplateSchemes
	}

	def listTemplateSchemesByStyle(style: AttendanceMonitoringPointStyle): Seq[AttendanceMonitoringTemplate] = {
		attendanceMonitoringDao.listTemplateSchemesByStyle(style)
	}

	def listStudentsPoints(student: StudentMember, departmentOption: Option[Department], academicYear: AcademicYear): Seq[AttendanceMonitoringPoint] = {
		student.mostSignificantCourseDetails.fold(Seq[AttendanceMonitoringPoint]())(scd => {
			val currentCourseStartDate = scd.beginDate
			val schemes = findSchemesForStudent(student.universityId, student.userId, departmentOption, academicYear)
			schemes.flatMap(_.points.asScala).filter(p =>
				p.startDate.isAfter(currentCourseStartDate) || p.startDate.isEqual(currentCourseStartDate)
			)
		})
	}

	def listStudentsPoints(studentData: AttendanceMonitoringStudentData, department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringPoint] = {
		val schemes = findSchemesForStudent(studentData.universityId, studentData.userId, Option(department), academicYear)
		schemes.flatMap(_.points.asScala).filter(p =>
			p.startDate.isAfter(studentData.scdBeginDate) || p.startDate.isEqual(studentData.scdBeginDate)
		)
	}

	private def findSchemesForStudent(universityId: String, userId: String, departmentOption: Option[Department], academicYear: AcademicYear): Seq[AttendanceMonitoringScheme] = {
		val user = new User(userId)
		user.setWarwickId(universityId)
		val schemes = benchmarkTask(s"membersHelper.findBy $universityId") {
			membersHelper.findBy(user)
		}
		departmentOption match {
			case Some(department) => schemes.filter(s => s.department == department && s.academicYear == academicYear)
			case None => schemes.filter(_.academicYear == academicYear)
		}
	}

	def getAllCheckpoints(point: AttendanceMonitoringPoint): Seq[AttendanceMonitoringCheckpoint] = {
		attendanceMonitoringDao.getAllCheckpoints(point)
	}

	def getCheckpoints(points: Seq[AttendanceMonitoringPoint], student: StudentMember, withFlush: Boolean = false): Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint] = {
		attendanceMonitoringDao.getCheckpoints(points, student, withFlush)
	}

	def getCheckpoints(points: Seq[AttendanceMonitoringPoint], students: Seq[StudentMember]): Map[StudentMember, Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint]] =
		attendanceMonitoringDao.getCheckpoints(points, students)

	def countCheckpointsForPoint(point: AttendanceMonitoringPoint): Int =
		attendanceMonitoringDao.countCheckpointsForPoint(point)
	

	def getAttendanceNote(student: StudentMember, point: AttendanceMonitoringPoint): Option[AttendanceMonitoringNote] = {
		attendanceMonitoringDao.getAttendanceNote(student, point)
	}

	def getAttendanceNoteMap(student: StudentMember): Map[AttendanceMonitoringPoint, AttendanceMonitoringNote] = {
		attendanceMonitoringDao.getAttendanceNoteMap(student)
	}

	def setAttendance(student: StudentMember, attendanceMap: Map[AttendanceMonitoringPoint, AttendanceState], user: CurrentUser): Seq[AttendanceMonitoringCheckpoint] = {
		setAttendance(student, attendanceMap, user.apparentId)
	}

	def setAttendance(student: StudentMember, attendanceMap: Map[AttendanceMonitoringPoint, AttendanceState], usercode: String, autocreated: Boolean = false): Seq[AttendanceMonitoringCheckpoint] = {
		val existingCheckpoints = getCheckpoints(attendanceMap.keys.toSeq, student)
		val checkpointsToDelete: Seq[AttendanceMonitoringCheckpoint] = attendanceMap.filter(_._2 == null).map(_._1).map(existingCheckpoints.get).flatten.toSeq
		val checkpointsToUpdate: Seq[AttendanceMonitoringCheckpoint] = attendanceMap.filter(_._2 != null).flatMap{case(point, state) =>
			val checkpoint = existingCheckpoints.getOrElse(point, {
				val checkpoint = new AttendanceMonitoringCheckpoint
				checkpoint.student = student
				checkpoint.point = point
				checkpoint.autoCreated = autocreated
				checkpoint
			})
			if (checkpoint.state != state) {
				checkpoint.state = state
				checkpoint.updatedBy = usercode
				checkpoint.updatedDate = DateTime.now
				Option(checkpoint)
			} else {
				None
			}
		}.toSeq
		attendanceMonitoringDao.removeCheckpoints(checkpointsToDelete)
		attendanceMonitoringDao.saveOrUpdateCheckpoints(checkpointsToUpdate)

		if (attendanceMap.keys.nonEmpty) {
			val scheme = attendanceMap.keys.head.scheme
			updateCheckpointTotal(student, scheme.department, scheme.academicYear)
		}

		checkpointsToUpdate
	}

	def updateCheckpointTotalsAsync(students: Seq[StudentMember], department: Department, academicYear: AcademicYear): Unit = {
		attendanceMonitoringDao.flush()
		students.foreach(student =>
			queue.send(new AttendanceMonitoringServiceUpdateCheckpointTotalMessage(
				student.universityId,
				department.code,
				academicYear.startYear.toString
		)))
	}

	def updateCheckpointTotal(student: StudentMember, department: Department, academicYear: AcademicYear): AttendanceMonitoringCheckpointTotal = {
		val points = benchmarkTask("listStudentsPoints") {
			listStudentsPoints(student, Option(department), academicYear)
		}
		val checkpointMap = getCheckpoints(points, student, withFlush = true)
		val allCheckpoints = checkpointMap.map(_._2)

		val unrecorded = points.diff(checkpointMap.keys.toSeq).count(_.startDate.isBefore(DateTime.now.toLocalDate))
		val missedUnauthorised = allCheckpoints.count(_.state == AttendanceState.MissedUnauthorised)
		val missedAuthorised = allCheckpoints.count(_.state == AttendanceState.MissedAuthorised)
		val attended = allCheckpoints.count(_.state == AttendanceState.Attended)

		val totals = attendanceMonitoringDao.getCheckpointTotal(student, Option(department), academicYear).getOrElse {
			val total = new AttendanceMonitoringCheckpointTotal
			total.student = student
			total.department = department
			total.academicYear = academicYear
			total
		}

		totals.unrecorded = unrecorded
		totals.unauthorised = missedUnauthorised
		totals.authorised = missedAuthorised
		totals.attended = attended
		totals.updatedDate = DateTime.now
		attendanceMonitoringDao.saveOrUpdate(totals)
		totals
	}

	def getCheckpointTotal(student: StudentMember, departmentOption: Option[Department], academicYear: AcademicYear): AttendanceMonitoringCheckpointTotal = {
		attendanceMonitoringDao.getCheckpointTotal(student, departmentOption, academicYear).getOrElse {
			val total = new AttendanceMonitoringCheckpointTotal
			total.student = student
			total.department = departmentOption.orNull
			total.academicYear = academicYear
			total
		}
	}

	def getCheckpointTotals(students: Seq[StudentMember], department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringCheckpointTotal] = {
		attendanceMonitoringDao.getCheckpointTotals(students, department, academicYear)
	}

	def generatePointsFromTemplateScheme(templateScheme: AttendanceMonitoringTemplate, academicYear: AcademicYear): Seq[AttendanceMonitoringPoint] = {
		val weeksForYear = termService.getAcademicWeeksForYear(academicYear.dateInTermOne).toMap
		val stubScheme = new AttendanceMonitoringScheme
		stubScheme.pointStyle = templateScheme.pointStyle
		stubScheme.academicYear = academicYear

		val attendanceMonitoringPoints =
			templateScheme.points.asScala.map { templatePoint =>
				val point = templatePoint.toPoint
				templateScheme.pointStyle match {
					case AttendanceMonitoringPointStyle.Date =>
						point.startDate = templatePoint.startDate.withYear(academicYear.getYear(templatePoint.startDate))
						point.endDate = templatePoint.endDate.withYear(academicYear.getYear(templatePoint.endDate))
					case AttendanceMonitoringPointStyle.Week =>
						point.startWeek = templatePoint.startWeek
						point.endWeek = templatePoint.endWeek
						point.startDate = weeksForYear(templatePoint.startWeek).getStart.withDayOfWeek(DayOfWeek.Monday.jodaDayOfWeek).toLocalDate
						point.endDate = weeksForYear(templatePoint.endWeek).getStart.withDayOfWeek(DayOfWeek.Monday.jodaDayOfWeek).toLocalDate.plusDays(6)
				}
				point.scheme = stubScheme
				point
			}
		attendanceMonitoringPoints
	}
}

trait AttendanceMonitoringMembershipHelpers {
	val membersHelper: UserGroupMembershipHelper[AttendanceMonitoringScheme]
}

// new up UGMHs which will Wire.auto() their dependencies
trait AttendanceMonitoringMembershipHelpersImpl extends AttendanceMonitoringMembershipHelpers {
	val membersHelper = new UserGroupMembershipHelper[AttendanceMonitoringScheme]("_members")
}

@Service("attendanceMonitoringService")
class AttendanceMonitoringServiceImpl
	extends AbstractAttendanceMonitoringService
	with AttendanceMonitoringMembershipHelpersImpl
	with AutowiringTermServiceComponent
	with AutowiringAttendanceMonitoringDaoComponent
	with AutowiringUserLookupComponent


class AttendanceMonitoringServiceListener extends QueueListener with InitializingBean with Logging with Daoisms
	with AutowiringAttendanceMonitoringServiceComponent with AutowiringProfileServiceComponent with AutowiringModuleAndDepartmentServiceComponent {

	var queue = Wire.named[Queue]("settingsSyncTopic")
	@Autowired var env: Environment = _

	private def convertArgsAndUpdate(universityId: String, departmentCode: String, academicStartYear: String) = transactional() {
		val memberConverter = new MemberUniversityIdConverter
		memberConverter.service = profileService
		val studentOption = memberConverter.convertRight(universityId) match {
			case student: StudentMember => Option(student)
			case _ => None
		}

		val departmentConverter = new DepartmentCodeConverter
		departmentConverter.service = moduleAndDepartmentService
		val departmentOption = departmentConverter.convertRight(departmentCode) match {
			case department: Department => Option(department)
			case _ => None
		}

		val academicYearOption = {
			try {
				Option(AcademicYear(academicStartYear.toInt))
			} catch {
				case e: NumberFormatException =>
					None
			}
		}
		if (studentOption.isEmpty) {
			logger.warn(s"Could not find student $universityId to update checkpoint total")
		} else if (departmentOption.isEmpty) {
			logger.warn(s"Could not find department $departmentCode to update checkpoint total")
		} else if (academicYearOption.isEmpty) {
			logger.warn(s"Could not find academic year $academicStartYear to update checkpoint total")
		} else {
			logger.debug(s"Updating checkpoint total from message for $universityId in $departmentCode for $academicStartYear")
			attendanceMonitoringService.updateCheckpointTotal(studentOption.get, departmentOption.get, academicYearOption.get)
		}
	}

	override def isListeningToQueue = env.acceptsProfiles("dev", "scheduling")
	override def onReceive(item: Any) {
		logger.debug(s"Synchronising item $item")
		item match {
			case msg: AttendanceMonitoringServiceUpdateCheckpointTotalMessage =>
				convertArgsAndUpdate(msg.getUniversityId, msg.getDepartmentCode, msg.getAcademicStartYear)
			case _ =>
		}
	}

	override def afterPropertiesSet() {
		queue.addListener(classOf[AttendanceMonitoringServiceUpdateCheckpointTotalMessage].getAnnotation(classOf[ItemType]).value, this)
	}
}

@ItemType("AttendanceMonitoringServiceUpdateCheckpointTotal")
@JsonAutoDetect
class AttendanceMonitoringServiceUpdateCheckpointTotalMessage {

	def this(thisUniversityId: String, thisDepartmentCode: String, thisAcademicStartYear: String) {
		this()
		universityId = thisUniversityId
		departmentCode = thisDepartmentCode
		academicStartYear = thisAcademicStartYear
	}

	@BeanProperty var universityId: String = _
	@BeanProperty var departmentCode: String = _
	@BeanProperty var academicStartYear: String = _
}
