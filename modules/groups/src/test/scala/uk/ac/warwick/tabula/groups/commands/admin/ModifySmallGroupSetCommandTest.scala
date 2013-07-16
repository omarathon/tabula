package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.{AcademicYear, TestBase}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroupFormat, SmallGroupSet}
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.JavaImports._


class ModifySmallGroupSetCommandTest extends TestBase{

	def getCommand(m:Module) = new ModifySmallGroupSetCommand(m) {
		def describe(d: Description) {}
		protected def applyInternal(): SmallGroupSet = new SmallGroupSet
	}

	@Test
	def copyFromSetsStateOnCommand(){
		val command = getCommand(new Module)
		val sourceSet = new SmallGroupSet()
		sourceSet.studentsCanSeeOtherMembers = true
		sourceSet.studentsCanSeeTutorName = true
		sourceSet.allowSelfGroupSwitching = true
		sourceSet.name = "test"
		sourceSet.academicYear = AcademicYear(2012)
		sourceSet.format = SmallGroupFormat.Lab
		sourceSet.allocationMethod = SmallGroupAllocationMethod.Manual


		command.copyFrom(sourceSet)
		command.studentsCanSeeOtherMembers should be (true)
		command.studentsCanSeeTutorName should be(true)
		command.allowSelfGroupSwitching should be(true)
		command.name should be("test")
		command.academicYear should be(AcademicYear(2012))
		command.format should be (SmallGroupFormat.Lab)
		command.allocationMethod should be (SmallGroupAllocationMethod.Manual)
	}

	@Test
	def copyToSetsStateOnSmallGroupSet(){

		val command = getCommand(new Module)

		command.studentsCanSeeOtherMembers = true
		command.studentsCanSeeTutorName = true
		command.allowSelfGroupSwitching = true
		command.name = "test"
		command.academicYear  = AcademicYear(2012)
		command.format  = SmallGroupFormat.Lab
		command.allocationMethod  = SmallGroupAllocationMethod.Manual

		val sourceSet = new SmallGroupSet()

		command.copyTo(sourceSet)

		sourceSet.studentsCanSeeOtherMembers should be (true)
		sourceSet.studentsCanSeeTutorName should be(true)
		sourceSet.allowSelfGroupSwitching should be(true)
		sourceSet.name should be("test")
		sourceSet.academicYear should be(AcademicYear(2012))
		sourceSet.format should be (SmallGroupFormat.Lab)
		sourceSet.allocationMethod should be (SmallGroupAllocationMethod.Manual)

	}

}
