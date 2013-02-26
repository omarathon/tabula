package uk.ac.warwick.tabula

import uk.ac.warwick.tabula.data.model._
import scala.collection.JavaConversions._

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
	
	def department(code:String, name:String = null) = {
		val d = new Department
		d.code = code
		d.name = Option(name).getOrElse("Department " + code)
		d
	}
	
	def module(code:String, name: String = null) = {
		val m = new Module
		m.code = code.toLowerCase
		m.name = Option(name).getOrElse("Module " + code)
		m
	}
	
	def assignment(name:String) = {
		val a = new Assignment
		a
	}
	
	def upstreamAssignment(departmentCode:String, number:Int) = {
        val a = new UpstreamAssignment
        a.name = "Assignment %d" format (number)
        a.departmentCode = departmentCode.toUpperCase
        a.moduleCode = "%s1%02d-30" format (departmentCode.toUpperCase, number)
        a.assessmentGroup = "A"
        a.sequence = "A%02d" format (number)
        a
    }
	
	def assessmentGroup(assignment:UpstreamAssignment) = {
		val group = new UpstreamAssessmentGroup
		group.academicYear = new AcademicYear(2012)
		group.assessmentGroup = assignment.assessmentGroup
		group.moduleCode = assignment.moduleCode
		group.occurrence = "A"
		group.members.staticIncludeUsers.addAll(Seq(
			"0123456",
			"0123457",
			"0123458"
		))
		group
	}
	
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
		member
	}
	
	def staff(universityId: String = "0123456", userId: String = "cuspxp", department: Department = null) = member(MemberUserType.Staff, universityId, userId, department)
	def student(universityId: String = "0123456", userId: String = "cuspxp", department: Department = null, studyDepartment: Department = null)	= { 
		val m = member(MemberUserType.Student, universityId, userId, department).asInstanceOf[StudentMember]
		m.studyDetails.studyDepartment = studyDepartment
		m
	}
		
}