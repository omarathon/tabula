package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.CurrentUser
import org.apache.poi.xssf.usermodel._
import uk.ac.warwick.tabula.commands.{ReadOnly, Unaudited, Command}
import uk.ac.warwick.tabula.permissions.Permissions
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.web.views.ExcelView
import org.apache.poi.ss.util.CellRangeAddressList

class AllocateStudentsTemplateCommand (val module: Module, val set: SmallGroupSet, viewer: CurrentUser)
		extends Command[ExcelView] with ReadOnly with Unaudited {

	mustBeLinked(set, module)
	PermissionCheck(Permissions.SmallGroups.Allocate, set)

	val groupLookupSheetName = "GroupLookup"
	val allocateSheetName = "AllocateStudents"

	def applyInternal() = {
		val workbook = generateWorkbook()
		new ExcelView("Allocation for " + set.name +  ".xlsx", workbook)
	}


	def generateWorkbook() = {
		val groups = set.groups.asScala.toList

		val workbook = new XSSFWorkbook()
		val sheet: XSSFSheet = generateAllocationSheet(workbook)

		generateGroupLookupSheet(workbook)
		generateGroupDropdowns(sheet, groups)

		val groupRange = groupLookupSheetName + "!$A2:$B" + (groups.length + 1)

		for (user <- set.members.users) {
			val currentRow = sheet.getLastRowNum() + 1
			val row = sheet.createRow(currentRow)
			row.createCell(0).setCellValue(user.getWarwickId)
			row.createCell(1).setCellValue(user.getFullName)
			row.createCell(3).setCellFormula("VLOOKUP($C" + (currentRow + 1) + ", " + groupRange + ", 2, FALSE)")
		}

		formatWorkbook(workbook)
		workbook
	}


	// attaches the data validation to the sheet
	def generateGroupDropdowns(sheet: XSSFSheet, groups: List[SmallGroup]) {
		val dropdownChoices = groups.map(_.name).toArray
		val dropdownRange = new CellRangeAddressList(1, groups.length, 2, 2)
		val validation = getDataValidation(dropdownChoices, sheet, dropdownRange)

		sheet.addValidationData(validation)
	}


	// Excel data validation - will only accept the values fed to this method, also puts a dropdown on each cell
	def getDataValidation(dropdownChoices: Array[String], sheet: XSSFSheet, addressList: CellRangeAddressList) = {
		val dvHelper = new XSSFDataValidationHelper(sheet)
		val dvConstraint = dvHelper.createExplicitListConstraint(dropdownChoices).asInstanceOf[XSSFDataValidationConstraint]
		val validation = dvHelper.createValidation(dvConstraint, addressList).asInstanceOf[XSSFDataValidation]

		validation.setShowErrorBox(true)
		validation
	}


	def generateGroupLookupSheet(workbook: XSSFWorkbook) {
		val groupSheet: XSSFSheet = workbook.createSheet(groupLookupSheetName)

		for (group <- set.groups.asScala) {
			val row = groupSheet.createRow(groupSheet.getLastRowNum() + 1)
			row.createCell(0).setCellValue(group.name)
			row.createCell(1).setCellValue(group.id)
		}

	}


	def generateAllocationSheet(workbook: XSSFWorkbook): XSSFSheet =  {
		val sheet = workbook.createSheet(allocateSheetName)

		// add header row
		val header = sheet.createRow(0)
		header.createCell(0).setCellValue("student_id")
		header.createCell(1).setCellValue("Student name")
		header.createCell(2).setCellValue("Group name")
		header.createCell(3).setCellValue("group_id")
		sheet
	}

	def formatWorkbook(workbook: XSSFWorkbook) = {
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