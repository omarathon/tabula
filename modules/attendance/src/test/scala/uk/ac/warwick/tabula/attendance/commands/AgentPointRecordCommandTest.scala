package uk.ac.warwick.tabula.attendance.commands

import javax.sql.DataSource

import org.hibernate.{Session, SessionFactory}
import uk.ac.warwick.tabula.attendance.commands.agent.old.{AgentPointRecordCommandState, AgentPointRecordDescription, AgentPointRecordPermissions, AgentPointRecordValidation, AgentPointRecordCommand}

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.{UserGroup, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpoint, MonitoringPoint, MonitoringPointAttendanceNote, AttendanceState, MonitoringPointSet}
import org.joda.time.{DateTimeConstants, DateTime}
import uk.ac.warwick.util.termdates.Term
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.convert.MonitoringPointIdConverter
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor
import org.hamcrest.Matchers._
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.commands.{Describable, SelfValidating, Appliable, DescriptionImpl}
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.events.EventListener
import uk.ac.warwick.spring.Wire

class AgentPointRecordCommandTest extends TestBase with FunctionalContextTesting with Mockito {

	import AgentPointRecordCommandTest.MinimalCommandContext

	trait CommandTestSupport extends AgentPointRecordCommandState with UserLookupComponent with TermServiceComponent with MonitoringPointServiceComponent with RelationshipServiceComponent {
		val userLookup = new MockUserLookup
		val termService = smartMock[TermService]
		val monitoringPointService = smartMock[MonitoringPointService]
		val relationshipService = smartMock[RelationshipService]
	}

	trait StateSetup {
		val agentMember = Fixtures.staff()
		val currentUser = new CurrentUser(realUser = agentMember.asSsoUser, apparentUser = agentMember.asSsoUser, profile = Some(agentMember))

		val tutorType = StudentRelationshipType("tutor", "tutor", "tutor", "tutee")

		val templateSet = new MonitoringPointSet
		templateSet.academicYear = AcademicYear(2013)
		val template = Fixtures.monitoringPoint(name = "Attend University", validFromWeek = 1, requiredFromWeek = 4)
		templateSet.add(template)

		val student1 = Fixtures.student("0000001", "student1")
		val student2 = Fixtures.student("0000002", "student2")
		val student3 = Fixtures.student("0000003", "student3")
		val student4 = Fixtures.student("0000004", "student4")

		val allStudents = Seq(student1, student2, student3, student4)

		val studentRel1 = StudentRelationship(agentMember, tutorType, student1)
		val studentRel2 = StudentRelationship(agentMember, tutorType, student2)
		val studentRel3 = StudentRelationship(agentMember, tutorType, student3)
		val studentRel4 = StudentRelationship(agentMember, tutorType, student4)

		val pointSet1 = new MonitoringPointSet
		val pointSet2 = new MonitoringPointSet
		pointSet1.academicYear = AcademicYear(2013)
		pointSet2.academicYear = AcademicYear(2013)

		val point1 = Fixtures.monitoringPoint(name = "Attend University", validFromWeek = 1, requiredFromWeek = 4)
		point1.id = "point1Id"
		pointSet1.add(point1)

		val point2 = Fixtures.monitoringPoint(name = "Attend University", validFromWeek = 1, requiredFromWeek = 4)
		point2.id = "point2Id"
		pointSet2.add(point2)
	}

	trait Fixture extends StateSetup {
		val command = new AgentPointRecordCommand(agentMember, currentUser, tutorType, template) with CommandTestSupport with AgentPointRecordValidation
	}

	@Test def init() { new Fixture {
		command.agent should be (agentMember)
		command.user should be (currentUser)
		command.relationshipType should be (tutorType)
		command.templateMonitoringPoint should be (template)
	}}

	trait AgentPointRecordWorld extends Fixture {
		command.relationshipService.listStudentRelationshipsWithMember(tutorType, agentMember) returns Seq(studentRel1, studentRel2, studentRel3, studentRel4)

		command.monitoringPointService.getPointSetForStudent(student1, AcademicYear(2013)) returns Some(pointSet1)
		command.monitoringPointService.getPointSetForStudent(student2, AcademicYear(2013)) returns Some(pointSet1)
		command.monitoringPointService.getPointSetForStudent(student3, AcademicYear(2013)) returns Some(pointSet1)
		command.monitoringPointService.getPointSetForStudent(student4, AcademicYear(2013)) returns Some(pointSet2)
		command.monitoringPointService.getPointById(point1.id) returns Some(point1)
		command.monitoringPointService.getPointById(point2.id) returns Some(point2)

		// Student3 has already been reported to SITS this term
		val nonReportedStudents = Seq(student1, student2, student4)

		command.monitoringPointService.findSimilarPointsForMembers(template, allStudents) returns Map(
			student1 -> Seq(point1),
			student2 -> Seq(point1),
			student3 -> Seq(point1),
			student4 -> Seq(point2)
		)

		// Student1 and Student3 already have a checkpoint
		val student1Point = Fixtures.monitoringCheckpoint(point1, student1, AttendanceState.MissedAuthorised)
		student1Point.updatedBy = "cuscav"
		student1Point.updatedDate = new DateTime(2013, DateTimeConstants.JANUARY, 13, 12, 49, 12, 0)

		// Point 1 has an associated note
		val student1PointNote = new MonitoringPointAttendanceNote
		student1PointNote.point = point1
		student1PointNote.student = student1
		student1PointNote.note = "Was having a wee"

		val student3Point = Fixtures.monitoringCheckpoint(point1, student3, AttendanceState.Attended)
		student3Point.updatedBy = "cuscav"
		student3Point.updatedDate = new DateTime(2013, DateTimeConstants.JANUARY, 13, 12, 49, 12, 0)

		val term = smartMock[Term]
		term.getTermTypeAsString returns "spring"

		command.termService.getTermFromAcademicWeekIncludingVacations(point1.validFromWeek, AcademicYear(2013)) returns term
		command.termService.getTermFromAcademicWeekIncludingVacations(point2.validFromWeek, AcademicYear(2013)) returns term

		command.termService.getAcademicWeekForAcademicYear(any[DateTime], any[AcademicYear]) returns 5

		command.monitoringPointService.findNonReported(allStudents, AcademicYear(2013), "spring") returns nonReportedStudents
		command.monitoringPointService.findNonReportedTerms(Seq(student1), AcademicYear(2013)) returns Seq("spring")
		command.monitoringPointService.findNonReportedTerms(Seq(student2), AcademicYear(2013)) returns Seq("spring")
		command.monitoringPointService.findNonReportedTerms(Seq(student3), AcademicYear(2013)) returns Nil
		command.monitoringPointService.findNonReportedTerms(Seq(student4), AcademicYear(2013)) returns Seq("spring")

		command.monitoringPointService.getCheckpointsByStudent(Seq(point1, point2)) returns Seq((student1, student1Point), (student3, student3Point))

		command.userLookup.registerUsers("cuscav")

		command.monitoringPointService.findAttendanceNotes(allStudents, Seq(point1, point2)) returns Seq(student1PointNote)
	}

	@Test def populate() { new AgentPointRecordWorld {
		command.populate()
		command.studentsState.asScala.mapValues { _.asScala } should be (Map(
			student1 -> Map(point1 -> AttendanceState.MissedAuthorised),
			student2 -> Map(point1 -> null),
			student4 -> Map(point2 -> null)
		))
		command.checkpointDescriptions should be (Map(
			student1 -> Map(point1 -> "Recorded by Cuscavy McCuscaverson, 12:49&#8194;Sun 13<sup>th</sup> January 2013"),
			student2 -> Map(point1 -> ""),
			student3 -> Map(point1 -> "Recorded by Cuscavy McCuscaverson, 12:49&#8194;Sun 13<sup>th</sup> January 2013"),
			student4 -> Map(point2 -> "")
		))
		command.attendanceNotes should be (Map(
			student1 -> Map(point1 -> student1PointNote)
		))
	}}

	@Test def onBind() { new AgentPointRecordWorld {
		command.populate()
		command.onBind(getErrors(command))
		command.studentsStateAsScala should be (Map(
			student1 -> Map(point1 -> AttendanceState.MissedAuthorised),
			student2 -> Map(point1 -> null),
			student4 -> Map(point2 -> null)
		))
	}}

	@Test def apply() { new AgentPointRecordWorld {
		command.populate()

		// Make it mutable
		command.studentsState = JHashMap(command.studentsState)

		// Un-record student1
		command.studentsState.put(student1, JHashMap(point1 -> null))

		// Record student4 as attended
		command.studentsState.put(student4, JHashMap(point2 -> AttendanceState.Attended))

		command.onBind(getErrors(command))

		val student4Point = Fixtures.monitoringCheckpoint(point2, student4, AttendanceState.Attended)
		command.monitoringPointService.saveOrUpdateCheckpointByMember(student4, point2, AttendanceState.Attended, agentMember) returns student4Point

		command.applyInternal() should be (Seq(student4Point))
		verify(command.monitoringPointService, times(1)).deleteCheckpoint(student1, point1)
	}}

	trait ValidationFixture extends AgentPointRecordWorld

	@Test def validatePasses() { new ValidationFixture {
		command.populate()

		val errors = getErrors(command)
		command.onBind(errors)
		command.validate(errors)

		errors.hasErrors should be (false)
		// TAB-2025
		verify(command.termService, times(0)).getTermFromAcademicWeek(any[Int], any[AcademicYear], any[Boolean])
	}}

	@Test def rejectPointNotInSet() { new ValidationFixture {
		// Make it mutable
		command.studentsState = JHashMap()

		// Try and record point2 for student1
		command.studentsState.put(student1, JHashMap(point1 -> null, point2 -> AttendanceState.Attended))

		val errors = getErrors(command)
		command.onBind(errors)
		command.validate(errors)

		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("studentsState[0000001][point2Id]")
		errors.getFieldError.getCodes should contain ("monitoringPoint.invalidStudent")
	}}

	@Test def rejectStudentAlreadyReportedThisTerm() { new ValidationFixture {
		// Make it mutable
		command.studentsState = JHashMap()

		// Try and record for student3
		command.studentsState.put(student3, JHashMap(point1 -> AttendanceState.Attended))

		val errors = getErrors(command)
		command.onBind(errors)
		command.validate(errors)

		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("studentsState[0000003][point1Id]")
		errors.getFieldError.getCodes should contain ("monitoringCheckpoint.student.alreadyReportedThisTerm")
	}}

	private def getErrors[A <: CommandTestSupport](command: A) = {
		val errors = new BindException(command, "command")
		val editor = new MonitoringPointIdConverter
		editor.service = command.monitoringPointService

		errors.getPropertyEditorRegistry.registerCustomEditor(classOf[MonitoringPoint], new AbstractPropertyEditor[MonitoringPoint]() {
			def fromString(id: String) = editor.convertRight(id)
			def toString(point: MonitoringPoint) = editor.convertLeft(point)
		})
		errors
	}

	@Test
	def permissionsRequireReadRelationshipAndViewStudents() { new StateSetup {
		val command = new AgentPointRecordPermissions with CommandTestSupport {
			val agent = agentMember
			val user = currentUser
			val relationshipType = tutorType
			val templateMonitoringPoint = template
		}

		command.relationshipService.listStudentRelationshipsWithMember(tutorType, agentMember) returns Seq(studentRel1, studentRel2, studentRel3, studentRel4)

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
		verify(checking, times(1)).PermissionCheck(Permissions.Profiles.StudentRelationship.Read(tutorType), agentMember)
		verify(checking, times(1)).PermissionCheckAll(Permissions.MonitoringPoints.Record, allStudents)
	}}

	@Test
	def describe() { new StateSetup {
		val dept = Fixtures.department("in")

		val command = new AgentPointRecordDescription with CommandTestSupport {
			val agent = agentMember
			val user = currentUser
			val relationshipType = tutorType
			val templateMonitoringPoint = template
		}

		command.studentsStateAsScala = Map(
			student1 -> Map(point1 -> AttendanceState.MissedAuthorised),
			student2 -> Map(point1 -> null),
			student4 -> Map(point2 -> null)
		)

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"checkpoints" -> Map(
				"0000001" -> Map("point1Id" -> "authorised"),
				"0000002" -> Map("point1Id" -> "null"),
				"0000004" -> Map("point2Id" -> "null")
			)
		))
	}}

	@Test
	def glueEverythingTogether() { inContext[MinimalCommandContext] { new StateSetup {
		Wire[RelationshipService].listStudentRelationshipsWithMember(tutorType, agentMember) returns Seq(studentRel1, studentRel2, studentRel3, studentRel4)

		val command = AgentPointRecordCommand(agentMember, currentUser, tutorType, template)

		command should be (anInstanceOf[Appliable[Seq[MonitoringCheckpoint]]])
		command should be (anInstanceOf[AgentPointRecordCommandState])
		command should be (anInstanceOf[AgentPointRecordPermissions])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[AgentPointRecordValidation])
		command should be (anInstanceOf[Describable[Seq[MonitoringCheckpoint]]])
	}}}

}

object AgentPointRecordCommandTest {
	class MinimalCommandContext extends FunctionalContext with Mockito {
		bean() { smartMock[Features] }
		bean() { smartMock[ProfileService] }
		bean() { smartMock[AssessmentService] }
		bean() { smartMock[RelationshipService] }
		bean() { smartMock[UserLookupService] }
		bean() { smartMock[ModuleAndDepartmentService] }
		bean() { smartMock[SmallGroupService] }
		bean() { smartMock[MonitoringPointService] }
		bean() { smartMock[TermService] }
		bean() { smartMock[EventListener] }
		bean() { smartMock[NotificationService] }
		bean() { smartMock[ScheduledNotificationService] }
		bean() { smartMock[MaintenanceModeService] }
		bean() {
			val permissionsService = mock[PermissionsService]
			permissionsService.ensureUserGroupFor(anArgThat(anything), anArgThat(anything))(anArgThat(anything)) returns UserGroup.ofUsercodes
			permissionsService
		}
		bean(){
			val sessionFactory = smartMock[SessionFactory]
			val session = smartMock[Session]
			sessionFactory.getCurrentSession returns session
			sessionFactory.openSession() returns session
			sessionFactory
		}
		bean("dataSource"){mock[DataSource]}
	}
}