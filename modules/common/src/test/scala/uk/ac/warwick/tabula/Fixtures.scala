package uk.ac.warwick.tabula

import scala.collection.JavaConversions._

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpoint, MonitoringCheckpointState, MonitoringPoint}
import org.joda.time.DateTime

// scalastyle:off magic.number
object Fixtures {

	def submission(universityId: String = "0123456", userId: String = "cuspxp") = {
		val s = new Submission
		s.universityId = universityId
		s.userId = userId
		s
	}

	def feedback(universityId: String = "0123456") = {
		val f = new Feedback
		f.universityId = universityId
		f
	}

	def extension(universityId: String = "0123456", userId: String = "cuspxp") = {
		val e = new Extension
		e.universityId = universityId
		e.userId = userId
		e
	}

	def markerFeedback(parent: Feedback) = {
		new MarkerFeedback(parent)
	}

	def department(code:String, name:String = null) = {
		val d = new Department
		d.code = code
		d.name = Option(name).getOrElse("Department " + code)
		d
	}

	def departmentWithId(code:String, name:String = null, id: String) = {
		val d = new Department
		d.code = code
		d.name = Option(name).getOrElse("Department " + code)
		d.id = id
		d
	}

	def module(code:String, name: String = null) = {
		val m = new Module
		m.code = code.toLowerCase
		m.name = Option(name).getOrElse("Module " + code)
		m
	}

	def route(code:String, name: String = null) = {
		val r = new Route
		r.code = code.toLowerCase
		r.name = Option(name).getOrElse("Route " + code)
		r
	}

	def course(code:String, name: String = null) = {
		val c = new Course
		c.code = code
		c.name = Option(name).getOrElse("Course " + code)
		c
	}

	def assignment(name:String) = {
		val a = new Assignment
		a.name = name
		a
	}

	def smallGroupSet(name:String) = {
		val s = new SmallGroupSet
		s.name = name
		s
	}

	def smallGroup(name:String) = {
		val s = new SmallGroup
		s.name = name
		s
	}

	def upstreamAssignment(departmentCode:String, number:Int) = {
		val a = new AssessmentComponent
		a.name = "Assignment %d" format number
		a.departmentCode = departmentCode.toUpperCase
		a.moduleCode = "%s1%02d-30" format (departmentCode.toUpperCase, number)
		a.assessmentGroup = "A"
		a.sequence = "A%02d" format number
		a.assessmentType = AssessmentType.Assignment
		a
	}

	def assessmentGroup(academicYear: AcademicYear, code: String, module: String, occurrence: String) = {
		val group = new UpstreamAssessmentGroup
		group.academicYear = academicYear
		group.assessmentGroup = code
		group.moduleCode = module
		group.occurrence = occurrence
		group.members.staticIncludeUsers.addAll(Seq(
			"0123456",
			"0123457",
			"0123458"
		))
		group
	}

	def assessmentGroup(assignment:AssessmentComponent): UpstreamAssessmentGroup =
		assessmentGroup(
			academicYear = new AcademicYear(2012),
			code = assignment.assessmentGroup,
			module = assignment.moduleCode + "-30",
			occurrence = "A")


	def markingWorkflow(name: String) = {
		val workflow = new MarkingWorkflow
		workflow.name = name
		workflow
	}

	def feedbackTemplate(name: String) = {
		val template = new FeedbackTemplate
		template.name = name
		template
	}

	def userSettings(userId: String = "cuspxp") = {
		val settings = new UserSettings
		settings.userId = userId
		settings
	}

	def member(userType: MemberUserType, universityId: String = "0123456", userId: String = "cuspxp", department: Department = null) = {
		val member = userType match {
			case MemberUserType.Student => new StudentMember
			case MemberUserType.Emeritus => new EmeritusMember
			case MemberUserType.Staff => new StaffMember
			case MemberUserType.Other => new OtherMember
		}
		member.universityId = universityId
		member.userId = userId
		member.userType = userType
		member.homeDepartment = department
		member.inUseFlag = "Active"
		member
	}

	def staff(universityId: String = "0123456", userId: String = "cuspxp", department: Department = null) =
		member(MemberUserType.Staff, universityId, userId, department).asInstanceOf[StaffMember]

	def sitsStatus(code: String = "F", shortName: String = "Fully enrolled", fullName: String = "Fully enrolled for this session") = {
		val status = new SitsStatus(code, shortName, fullName)
		status
	}

	def student(universityId: String = "0123456", userId: String = "cuspxp", department: Department = null, courseDepartment: Department = null, sprStatus: SitsStatus = null)	= {
		val m = member(MemberUserType.Student, universityId, userId, department).asInstanceOf[StudentMember]

		val studentCourseDetails = new StudentCourseDetails(m, m.universityId + "/1")
		studentCourseDetails.student = m
		studentCourseDetails.sprCode = m.universityId + "/1"
		studentCourseDetails.department = courseDepartment
		studentCourseDetails.mostSignificant = true

		studentCourseDetails.sprStatus = sprStatus

		m.studentCourseDetails.add(studentCourseDetails)
		m
	}

	def studentCourseYearDetails(academicYear: AcademicYear = AcademicYear.guessByDate(DateTime.now)) = {
		val scyd = new StudentCourseYearDetails
		scyd.academicYear = academicYear
		scyd
	}

	def monitoringPoint(name: String = "name", validFromWeek: Int = 0, requiredFromWeek: Int = 0) = {
		val point = new MonitoringPoint
		point.name = name
		point.validFromWeek = validFromWeek
		point.requiredFromWeek = requiredFromWeek
		point
	}

	def memberNoteWithId(note: String, student: Member, id: String ) = {
		val memberNote = new MemberNote
		memberNote.note = note
		memberNote.member = student
		memberNote.id = id
		memberNote
	}

	def memberNote(note: String, student: Member ) = {
		val memberNote = new MemberNote
		memberNote.note = note
		memberNote.member = student
		memberNote
	}

	def monitoringCheckpoint(point: MonitoringPoint, studentCourseDetails: StudentCourseDetails, state: MonitoringCheckpointState) = {
		val checkpoint = new MonitoringCheckpoint
		checkpoint.point = point
		checkpoint.studentCourseDetail = studentCourseDetails
		checkpoint.state = state
		checkpoint
	}

	def meetingRecordApproval(state: MeetingApprovalState) = {
		val approval = new MeetingRecordApproval
		approval.state = state
		approval
	}
}
