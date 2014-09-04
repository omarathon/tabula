package uk.ac.warwick.tabula.groups.commands

import org.joda.time.DateTime
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupEvent, SmallGroupEventOccurrence, SmallGroupSet}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringEventAttendanceService, AttendanceMonitoringEventAttendanceServiceComponent}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

class RecordAttendanceCommandTest extends TestBase with Mockito {
	
	val smallGroupEventOccurrence = mock[SmallGroupEventOccurrence]
	val mockCurrentUser = mock[CurrentUser]

	// Implements the dependencies declared by the command
	trait CommandTestSupport extends SmallGroupServiceComponent with UserLookupComponent
	with ProfileServiceComponent with TermServiceComponent with MonitoringPointGroupProfileServiceComponent
	with FeaturesComponent with AttendanceMonitoringEventAttendanceServiceComponent {
		val smallGroupService = mock[SmallGroupService]
		val userLookup = mock[UserLookupService]
		val profileService = mock[ProfileService]
		val termService = mock[TermService]
		val monitoringPointGroupProfileService = mock[MonitoringPointGroupProfileService]
		val attendanceMonitoringEventAttendanceService = smartMock[AttendanceMonitoringEventAttendanceService]
		val features = emptyFeatures
		
		def apply(): SmallGroupEventOccurrence = {
			smallGroupEventOccurrence
		}
	}

	@Test
	def commandApply() = withCurrentUser(mockCurrentUser) {
		val event = new SmallGroupEvent
		event.group = new SmallGroup
		event.group.groupSet = new SmallGroupSet
		
		val week = 1
		val user = new User("abcde")
		user.setWarwickId("1234567")

		val command = new RecordAttendanceCommand(event, week, currentUser) with CommandTestSupport
		command.studentsState.put("1234567", AttendanceState.Attended)
		command.applyInternal()

		there was no(command.userLookup).getUsersByUserIds(Seq("abcde").asJava)
		there was one(command.smallGroupService).saveOrUpdateAttendance("1234567", event, week, AttendanceState.Attended, currentUser)
	}
	
	trait Fixture {
		val invalidUser = new User("invalid")
		invalidUser.setWarwickId("invalid")
		invalidUser.setFoundUser(false)
		
		val missingUser = new User("missing")
		missingUser.setWarwickId("missing")
		missingUser.setFoundUser(true)
		missingUser.setWarwickId("missing")
		
		val validUser = new User("valid")
		validUser.setWarwickId("valid")
		validUser.setFoundUser(true)
		validUser.setWarwickId("valid")
		
		val event = new SmallGroupEvent()
		val group = new SmallGroup()
		event.group = group
		val students = UserGroup.ofUsercodes
		group.students = students
		
		val set = new SmallGroupSet()
		group.groupSet = set
		set.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		
		val week = 1

		val command = new RecordAttendanceCommand(event, week, currentUser) with CommandTestSupport with RecordAttendanceCommandValidation with SmallGroupEventInFutureCheck 
		
		// Current week is 1, so allowed to record
		command.termService.getAcademicWeekForAcademicYear(isA[DateTime], isEq(set.academicYear)) returns 1
		
		command.userLookup.getUserByWarwickUniId(invalidUser.getWarwickId) returns invalidUser
		command.userLookup.getUserByWarwickUniId(missingUser.getWarwickId) returns missingUser
		command.userLookup.getUserByWarwickUniId(validUser.getWarwickId) returns validUser
		students.userLookup = command.userLookup
		students.userLookup.getUsersByUserIds(JArrayList(validUser.getUserId)) returns JMap(validUser.getUserId -> validUser)
		
		students.addUserId(validUser.getUserId)
	}
	
	@Test
	def validateInvalid() = withCurrentUser(mockCurrentUser) {
		new Fixture {
			command.studentsState = JHashMap()
			command.studentsState.put(invalidUser.getWarwickId, AttendanceState.Attended)
			command.studentsState.put(validUser.getWarwickId, AttendanceState.Attended)
			
			val errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be {true}
			errors.getFieldError("studentsState").getArguments should have size 1
		}
	}
	
	@Test
	def validateMissing() = withCurrentUser(mockCurrentUser) {
		new Fixture {
			command.studentsState = JHashMap()
			command.studentsState.put(missingUser.getWarwickId, AttendanceState.Attended)
			command.studentsState.put(validUser.getWarwickId, AttendanceState.Attended)
			
			val errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be {false} // TAB-1791
		}
	}
	
	@Test
	def validateValid() = withCurrentUser(mockCurrentUser) {
		new Fixture {
			command.studentsState = JHashMap()
			command.studentsState.put(validUser.getWarwickId, AttendanceState.Attended)
			
			val errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be {false}
		}
	}

}
