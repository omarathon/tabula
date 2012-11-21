package uk.ac.warwick.tabula

import uk.ac.warwick.tabula.data.model._
import scala.collection.JavaConversions._

object Fixtures {
	
	def submission() = {
		val s = new Submission
		s.universityId = "0123456"
		s.userId = "cuspxp"
		s
	}
	
	def department(code:String, name:String) = {
		val d = new Department
		d.code = code
		d.name = name
		d
	}
	
	def module(code:String, name: String=null) = {
		val m = new Module
		m.code = code.toLowerCase
		m.name = Option(name).getOrElse("Module " + code)
		m
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
		
}