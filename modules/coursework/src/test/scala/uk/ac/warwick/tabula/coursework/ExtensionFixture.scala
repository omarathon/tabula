package uk.ac.warwick.tabula.coursework

import uk.ac.warwick.tabula.data.model.{StaffMember, StudentMember, Module, Assignment}
import uk.ac.warwick.tabula.data.model.forms.Extension
import org.joda.time.DateTime

trait ExtensionFixture {

	val studentMember = new StudentMember
	studentMember.universityId = "student"
	val student = studentMember.asSsoUser

	val adminMember = new StaffMember
	adminMember.universityId = "admin"
	val admin= adminMember.asSsoUser

	val module = new Module
	module.code = "xxx"
	val assignment = new Assignment
	assignment.id = "123"
	assignment.closeDate = new DateTime(2013, 8, 1, 12, 0)
	assignment.module = module

	val extension = new Extension(student.getWarwickId)
	extension.expiryDate = new DateTime(2013, 8, 23, 12, 0)

	//extension.reason = "My hands have turned to flippers. Like the ones that dolphins have. It makes writing and typing super hard. Pity me."
	//extension.approvalComments = "That sounds awful. Have an extra month. By then you should be able to write as well as any Cetacea."
	//extension.approved = true
	//extension.approvedOn = new DateTime(2012, 7, 22, 14, 42)
	extension.assignment = assignment
	assignment.extensions add extension
}
