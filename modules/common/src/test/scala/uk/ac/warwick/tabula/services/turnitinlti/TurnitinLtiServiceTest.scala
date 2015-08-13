package uk.ac.warwick.tabula.services.turnitinlti

import uk.ac.warwick.tabula.{AcademicYear, Fixtures, TestBase}
import uk.ac.warwick.tabula.data.model.FileAttachment

class TurnitinLtiServiceTest extends TestBase {

	private trait Fixture {
		val file = new FileAttachment

		val assignment = Fixtures.assignment("1500 word assignment")
		val module = Fixtures.module("ab101", "First year module")
		assignment.module = module
		assignment.academicYear = new AcademicYear(2014)
		assignment.id = "12345"
	}

	@Test def testValidFileType() { new Fixture {
		file.name = "test.doc"
		TurnitinLtiService.validFileType(file) should be {true}
	}}

	@Test def testInvalidFileType() { new Fixture {
		file.name = "test.gif"
		TurnitinLtiService.validFileType(file) should be {false}
	}}

	@Test def generatedClassId() { new Fixture {
		val prefix = "TestModule"
		TurnitinLtiService.classIdFor(assignment, prefix).value should be("TestModule-ab101")
	}}

	@Test def generatedAssignmentId() { new Fixture {
		TurnitinLtiService.assignmentIdFor(assignment).value should be(s"${TurnitinLtiService.AssignmentPrefix}12345")
	}}

	@Test def generatedClassName() { new Fixture {
		TurnitinLtiService.classNameFor(assignment).value should be("AB101-First year module")
	}}

	@Test def generatedAssignmentName() { new Fixture {
		TurnitinLtiService.assignmentNameFor(assignment).value should be("1500 word assignment(14/15)")
	}}

}
