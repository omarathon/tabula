package uk.ac.warwick.tabula.commands.cm2.marksmanagement

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{DegreeType, Department}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

sealed abstract class DepartmentMarksState (val value: String)
case object DepartmentMarksStateClosed extends DepartmentMarksState("closed")
case object DepartmentMarksStateThisYearOnly extends DepartmentMarksState("openCurrent")
case object DepartmentMarksStateThisYearAndLastYear extends DepartmentMarksState("openCurrentAndPrevious")

object MarksOpenAndCloseDepartmentsCommand {
	def apply() =
		new MarksOpenAndCloseDepartmentsCommandInternal
		with ComposableCommand[DegreeType]
		with AutowiringModuleAndDepartmentServiceComponent
		with MarksPopulateOpenAndCloseDepartmentsCommand
		with MarksOpenAndCloseDepartmentsCommandState
		with MarksOpenAndCloseDepartmentsCommandPermissions
		with MarksOpenAndCloseDepartmentsCommandDescription
}

class MarksOpenAndCloseDepartmentsCommandInternal extends CommandInternal[DegreeType] {

	self: MarksOpenAndCloseDepartmentsCommandState with ModuleAndDepartmentServiceComponent =>

	def applyInternal(): DegreeType = {
		if (updatePostgrads){
			updateDepartments(pgMappings, DegreeType.Postgraduate)
			DegreeType.Postgraduate
		} else {
			updateDepartments(ugMappings, DegreeType.Undergraduate)
			DegreeType.Undergraduate
		}
	}

	private def updateDepartments(mapping: JMap[String, String], degreeType: DegreeType ) {
		mapping.asScala.foreach {
			case (key, value) => {
				val dept: Department = moduleAndDepartmentService.getDepartmentByCode(key).get
				dept.setUploadMarksToSitsForYear(currentAcademicYear, degreeType, thisYearOpen(value))
				dept.setUploadMarksToSitsForYear(previousAcademicYear, degreeType, lastYearOpen(value))
			}
		}
	}

	private def thisYearOpen(deptStateValue: String) = deptStateValue != DepartmentMarksStateClosed.value
	private def lastYearOpen(deptStateValue: String) = deptStateValue ==  DepartmentMarksStateThisYearAndLastYear.value

}

trait MarksOpenAndCloseDepartmentsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Marks.MarksManagement)
	}
}

trait MarksOpenAndCloseDepartmentsCommandDescription extends Describable[DegreeType] {
	self: MarksOpenAndCloseDepartmentsCommandState =>

	override lazy val eventName: String = "MarksOpenAndCloseDepartments"

	def describe(d: Description): Unit = d.properties(
		"degreeType" -> {
			if (updatePostgrads) DegreeType.Postgraduate
			else DegreeType.Undergraduate
		}
	)
}

trait MarksOpenAndCloseDepartmentsCommandState {
	self: ModuleAndDepartmentServiceComponent =>

	lazy val currentAcademicYear: AcademicYear = AcademicYear.now()
	lazy val previousAcademicYear: AcademicYear = currentAcademicYear.previous

	lazy val departments: Seq[Department] = moduleAndDepartmentService.allRootDepartments
	var ugMappings: JMap[String, String] = JHashMap()
	var pgMappings: JMap[String, String] = JHashMap()

	var updatePostgrads: Boolean = _
}

trait MarksPopulateOpenAndCloseDepartmentsCommand extends PopulateOnForm {

	self: MarksOpenAndCloseDepartmentsCommandState =>

	override def populate(): Unit = {

		ugMappings = departments.map {d =>
			d.code -> getState(d, DegreeType.Undergraduate).value
		}.toMap.asJava
		pgMappings = departments.map {d =>
			d.code -> getState(d, DegreeType.Postgraduate).value
		}.toMap.asJava

	}

	def getState(department: Department, degreeType: DegreeType): DepartmentMarksState = {

		val canUploadThisYear = department.canUploadMarksToSitsForYear(currentAcademicYear, degreeType)
		val canUploadLastYear = department.canUploadMarksToSitsForYear(previousAcademicYear, degreeType)

		if (canUploadThisYear && canUploadLastYear) DepartmentMarksStateThisYearAndLastYear
		else if (canUploadThisYear) DepartmentMarksStateThisYearOnly
		else DepartmentMarksStateClosed

	}
}
