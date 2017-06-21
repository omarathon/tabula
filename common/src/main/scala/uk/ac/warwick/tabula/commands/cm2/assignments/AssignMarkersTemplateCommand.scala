package uk.ac.warwick.tabula.commands.cm2.assignments

import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.apache.poi.xssf.usermodel.{XSSFDataValidation, XSSFDataValidationConstraint, XSSFDataValidationHelper}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.ListMarkerAllocationsCommand.{Marker, _}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.views.ExcelView

import scala.collection.immutable.SortedSet


object AssignMarkersTemplateCommand {
	def apply(assignment: Assignment) = new AssignMarkersTemplateCommandInternal(assignment)
		with ComposableCommand[ExcelView]
		with AssignMarkersTemplatePermissions
		with ReadOnly
		with Unaudited
		with AutowiringUserLookupComponent
		with AutowiringCM2MarkingWorkflowServiceComponent
		with AutowiringAssessmentMembershipServiceComponent

	val StudentUsercode = "Student usercode"
	val StudentName = "Student name"
	val MarkerUsercode = "Marker usercode"
	val MarkerName = "Marker name"
}

class AssignMarkersTemplateCommandInternal(val assignment: Assignment) extends CommandInternal[ExcelView]
	with AssignMarkersTemplateState with FetchMarkerAllocations {

	this: UserLookupComponent with CM2MarkingWorkflowServiceComponent with AssessmentMembershipServiceComponent =>

	def applyInternal(): ExcelView = {
		val existingAllocations = fetchAllocations(assignment)

		val workbook = new SXSSFWorkbook
		val style = workbook.createCellStyle
		val format = workbook.createDataFormat
		// using an @ sets text format (from BuiltinFormats.class)
		style.setDataFormat(format.getFormat("@"))

		for(role <- existingAllocations.keys) {
			val markers = existingAllocations.markers.getOrElse(role, SortedSet[Marker]())
			val allocationsForMarkers = existingAllocations.allocations.getOrElse(role, Map())
			val getStudentsMarker: Student => Option[Marker] = (student: Student) => allocationsForMarkers
				.find{ case(_, students) => students.contains(student)}
				.map{ case(marker, _) => marker }

			// create allocation sheets and headers
			val sheet = workbook.createSheet(role)
			sheet.trackAllColumnsForAutoSizing()

			val header = sheet.createRow(0)
			header.createCell(0).setCellValue(AssignMarkersTemplateCommand.StudentUsercode)
			header.createCell(1).setCellValue(AssignMarkersTemplateCommand.StudentName)
			header.createCell(2).setCellValue(AssignMarkersTemplateCommand.MarkerName)
			header.createCell(3).setCellValue(AssignMarkersTemplateCommand.MarkerUsercode)
			0 to 3 foreach  { col => // set style on all columns
				sheet.setDefaultColumnStyle(col, style)
				sheet.autoSizeColumn(col)
			}
			sheet.setColumnWidth(3, 7000) // set ID column to be wider

			// create the marker lookup sheet
			val lookupSheetName = s"${role.replaceAll(" ", "").toLowerCase}lookup"
			val lookupSheet = workbook.createSheet(lookupSheetName)
			for (marker <- markers) {
				val row = lookupSheet.createRow(lookupSheet.getLastRowNum + 1)
				row.createCell(0).setCellValue(marker.getFullName)
				row.createCell(1).setCellValue(marker.getUserId)
			}

			// add dropdown and validation to the main sheet
			val dropdownRange = new CellRangeAddressList(1, existingAllocations.allStudents.size, 2, 2)
			val dvHelper = new XSSFDataValidationHelper(null)
			val dvConstraint = dvHelper.createFormulaListConstraint(
				lookupSheetName + "!$A$2:$A$" + (markers.size + 1)
			).asInstanceOf[XSSFDataValidationConstraint]
			val validation = dvHelper.createValidation(dvConstraint, dropdownRange).asInstanceOf[XSSFDataValidation]
			validation.setShowErrorBox(true)
			sheet.addValidationData(validation)

			// populate existing allocations
			for (student <- existingAllocations.allStudents) {
				val markerLookupRange = lookupSheetName + "!$A2:$B" + (markers.size + 1)
				val row = sheet.createRow(sheet.getLastRowNum + 1)
				row.createCell(0).setCellValue(student.getUserId)
				row.createCell(1).setCellValue(student.getFullName)
				val marker = getStudentsMarker(student)
				row.createCell(2).setCellValue(marker.map(_.getFullName).getOrElse(""))
				row.createCell(3).setCellFormula(
					"IF(TRIM($C" + (row.getRowNum + 1) + ")<>\"\", VLOOKUP($C" + (row.getRowNum + 1) + ", " + markerLookupRange + ", 2, FALSE), \" \")"
				)
			}
		}
		new ExcelView("Allocation for " + assignment.name + ".xlsx", workbook)
	}
}

trait AssignMarkersTemplatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AssignMarkersTemplateState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Assignment.Update, assignment.module)
	}
}

trait AssignMarkersTemplateState {
	val assignment: Assignment
}