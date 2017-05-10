package uk.ac.warwick.tabula.commands.groups.admin.reusable

import org.apache.poi.ss.usermodel.{Cell, DataValidation, Row, Sheet}
import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.views.ExcelView

import scala.collection.JavaConverters._

object AllocateStudentsToDepartmentalSmallGroupsTemplateCommand {
	def apply(department: Department, set: DepartmentSmallGroupSet) =
		new AllocateStudentsToDepartmentalSmallGroupsTemplateCommandInternal(department, set)
			with AutowiringProfileServiceComponent
			with ComposableCommand[ExcelView]
			with AllocateStudentsToDepartmentalSmallGroupsTemplatePermissions
			with ReadOnly with Unaudited
}

class AllocateStudentsToDepartmentalSmallGroupsTemplateCommandInternal(val department: Department, val set: DepartmentSmallGroupSet)
	extends CommandInternal[ExcelView] with AllocateStudentsToDepartmentalSmallGroupsTemplateCommandState {

	self:  ProfileServiceComponent =>
	val groupLookupSheetName = "GroupLookup"
	val allocateSheetName = "AllocateStudents"
	val sheetPassword = "roygbiv"

	def applyInternal(): ExcelView = {
		val workbook = generateWorkbook()
		new ExcelView("Allocation for " + set.name +  ".xlsx", workbook)
	}

	def generateWorkbook(): SXSSFWorkbook = {
		val groups = set.groups.asScala.toList
		val setUsers = set.allStudents
		val workbook = new SXSSFWorkbook
		val sheet = generateAllocationSheet(workbook)
		generateGroupLookupSheet(workbook)
		generateGroupDropdowns(sheet, groups, setUsers.size)
		val groupLookupRange = groupLookupSheetName + "!$A2:$B" + (groups.length + 1)
		setUsers.foreach { user =>
			val row = sheet.createRow(sheet.getLastRowNum + 1)

			// put the student details into the cells
			row.createCell(0).setCellValue(user.getWarwickId)
			row.createCell(1).setCellValue(user.getFullName)
			val groupNameCell = createUnprotectedCell(workbook, row, 2) // unprotect cell for the dropdown group name

			// If this user is already in a group, prefill
			groups.find { _.students.includesUser(user) }.foreach { group =>
				groupNameCell.setCellValue(group.name)
			}
			row.createCell(3).setCellFormula(
				"IF(ISTEXT($C" + (row.getRowNum + 1) + "), VLOOKUP($C" + (row.getRowNum + 1) + ", " + groupLookupRange + ", 2, FALSE), \" \")"
			)
		}

		formatWorkbook(workbook)
		workbook
	}

	def createUnprotectedCell(workbook: SXSSFWorkbook, row: Row, col: Int, value: String = ""): Cell = {
		val lockedCellStyle = workbook.createCellStyle()
		lockedCellStyle.setLocked(false)
		val cell = row.createCell(col)
		cell.setCellValue(value)
		cell.setCellStyle(lockedCellStyle)
		cell
	}

	// attaches the data validation to the sheet
	def generateGroupDropdowns(sheet: Sheet, groups: Seq[_], totalStudents: Int) {
		var lastRow =  totalStudents
		if(lastRow == 0) lastRow = 1
		val dropdownRange = new CellRangeAddressList(1, lastRow, 2, 2)
		val validation = getDataValidation(groups, sheet, dropdownRange)

		sheet.addValidationData(validation)
	}

	// Excel data validation - will only accept the values fed to this method, also puts a dropdown on each cell
	def getDataValidation(groups: Seq[_], sheet: Sheet, addressList: CellRangeAddressList): DataValidation = {
		val dvHelper = new XSSFDataValidationHelper(null)
		val dvConstraint = dvHelper.createFormulaListConstraint(groupLookupSheetName + "!$A$2:$A$" + (groups.length + 1))
		val validation = dvHelper.createValidation(dvConstraint, addressList)

		validation.setShowErrorBox(true)
		validation
	}

	def generateGroupLookupSheet(workbook: SXSSFWorkbook): Sheet = {
		val groupSheet = workbook.createSheet(groupLookupSheetName)

		for (group <- set.groups.asScala) {
			val row = groupSheet.createRow(groupSheet.getLastRowNum + 1)
			row.createCell(0).setCellValue(group.name)
			row.createCell(1).setCellValue(group.id)
		}

		groupSheet.protectSheet(sheetPassword)
		groupSheet
	}

	def generateAllocationSheet(workbook: SXSSFWorkbook): Sheet =  {
		val sheet = workbook.createSheet(allocateSheetName)
		sheet.trackAllColumnsForAutoSizing()

		// add header row
		val header = sheet.createRow(0)
		header.createCell(0).setCellValue("student_id")
		header.createCell(1).setCellValue("Student name")
		header.createCell(2).setCellValue("Group name")
		header.createCell(3).setCellValue("group_id")

		// using apache-poi, we can't protect certain cells - rather we have to protect
		// the entire sheet and then unprotect the ones we want to remain editable
		sheet.protectSheet(sheetPassword)
		sheet
	}

	def formatWorkbook(workbook: SXSSFWorkbook): Unit = {
		val style = workbook.createCellStyle
		val format = workbook.createDataFormat

		// using an @ sets text format (from BuiltinFormats.class)
		style.setDataFormat(format.getFormat("@"))

		val sheet = workbook.getSheet(allocateSheetName)

		// set style on all columns
		0 to 3 foreach  {
			col => sheet.setDefaultColumnStyle(col, style)
			sheet.autoSizeColumn(col)
		}

		// set ID column to be wider
		sheet.setColumnWidth(3, 7000)

	}
}

trait AllocateStudentsToDepartmentalSmallGroupsTemplateCommandState {
	def department: Department
	def set: DepartmentSmallGroupSet
}

trait AllocateStudentsToDepartmentalSmallGroupsTemplatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AllocateStudentsToDepartmentalSmallGroupsTemplateCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, department)
		p.PermissionCheck(Permissions.SmallGroups.Allocate, mandatory(set))
	}
}