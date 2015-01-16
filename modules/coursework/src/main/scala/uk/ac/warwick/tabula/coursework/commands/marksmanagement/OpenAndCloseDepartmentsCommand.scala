package uk.ac.warwick.tabula.coursework.commands.marksmanagement

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{DegreeType, Department}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringTermServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._

sealed abstract class DepartmentState (val value: String)
case object DepartmentStateClosed extends DepartmentState("closed")
case object DepartmentStateThisYearOnly extends DepartmentState("openCurrent")
case object DepartmentStateThisYearAndLastYear extends DepartmentState("openCurrentAndPrevious")

object OpenAndCloseDepartmentsCommand {
	def apply() =
		new OpenAndCloseDepartmentsCommandInternal
		with ComposableCommand[DegreeType]
		with AutowiringTermServiceComponent
		with AutowiringModuleAndDepartmentServiceComponent
		with PopulateOpenAndCloseDepartmentsCommand
		with OpenAndCloseDepartmentsCommandState
		with OpenAndCloseDepartmentsCommandPermissions
		with OpenAndCloseDepartmentsCommandDescription
}

class OpenAndCloseDepartmentsCommandInternal extends CommandInternal[DegreeType] {

	self: OpenAndCloseDepartmentsCommandState with ModuleAndDepartmentServiceComponent =>

	def applyInternal() = {
		var degreeType: DegreeType = DegreeType.Postgraduate
		if (updatePostgrads){
			pgMappings.asScala.foreach {
				case(key, value) => {
					val dept: Department = moduleAndDepartmentService.getDepartmentByCode(key).getOrElse(throw new NoSuchElementException)
					dept.setUploadMarksToSitsForYear(previousAcademicYear, DegreeType.Postgraduate,	value == DepartmentStateThisYearAndLastYear.value)
					dept.setUploadMarksToSitsForYear(currentAcademicYear, DegreeType.Postgraduate, value != DepartmentStateClosed.value)
				}
			}
		} else {
			degreeType = DegreeType.Undergraduate
			ugMappings.asScala.foreach {
				case(key, value) => {
					val dept: Department = moduleAndDepartmentService.getDepartmentByCode(key).getOrElse(throw new NoSuchElementException)
					dept.setUploadMarksToSitsForYear(previousAcademicYear, degreeType, value == DepartmentStateThisYearAndLastYear.value)
					dept.setUploadMarksToSitsForYear(currentAcademicYear, degreeType, value != DepartmentStateClosed.value)
				}
			}
		}
		degreeType
	}
}

trait OpenAndCloseDepartmentsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Marks.MarksManagement)
	}
}

trait OpenAndCloseDepartmentsCommandDescription extends Describable[DegreeType] {

	self: OpenAndCloseDepartmentsCommandState =>

	def describe(d: Description) = d.properties(
		"degreeType" -> {
			if (updatePostgrads) DegreeType.Postgraduate
			else DegreeType.Undergraduate
		}
	)
}

trait OpenAndCloseDepartmentsCommandState {

	self: TermServiceComponent with ModuleAndDepartmentServiceComponent =>

	lazy val currentAcademicYear = AcademicYear.findAcademicYearContainingDate(DateTime.now, termService)
	lazy val previousAcademicYear = currentAcademicYear.-(1)

	lazy val departments: Seq[Department] = moduleAndDepartmentService.allRootDepartments
	var ugMappings: JMap[String, String] = JHashMap()
	var pgMappings: JMap[String, String] = JHashMap()

	var updatePostgrads: Boolean = _
}

trait PopulateOpenAndCloseDepartmentsCommand extends PopulateOnForm {

	self: OpenAndCloseDepartmentsCommandState =>

	override def populate() = {

		ugMappings = departments.map {d =>
			d.code -> getState(d, DegreeType.Undergraduate).value
		}.toMap.asJava
		pgMappings = departments.map {d =>
			d.code -> getState(d, DegreeType.Postgraduate).value
		}.toMap.asJava

	}

	def getState(department: Department, degreeType: DegreeType): DepartmentState = {

		val canUploadThisYear = department.canUploadMarksToSitsForYear(currentAcademicYear, degreeType)
		val canUploadLastYear = department.canUploadMarksToSitsForYear(previousAcademicYear, degreeType)

		if (canUploadThisYear && canUploadLastYear) DepartmentStateThisYearAndLastYear
		else if (canUploadThisYear) DepartmentStateThisYearOnly
		else DepartmentStateClosed

	}
}
