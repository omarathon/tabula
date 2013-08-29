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
	val spreadsheetRows = 1000
	val sheetPassword = "roygbiv"

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

		val groupLookupRange = groupLookupSheetName + "!$A2:$B" + (groups.length + 1)
		val userIterator = set.members.users.iterator

		while (sheet.getLastRowNum < spreadsheetRows) {

			val row = sheet.createRow(sheet.getLastRowNum + 1)

			// put the student details into the cells
			// otherwise create blank unprotected cells for the user to enter
			if(userIterator.hasNext) {
				val user = userIterator.next
				val thisUser = user
				row.createCell(0).setCellValue(thisUser.getWarwickId)
				row.createCell(1).setCellValue(thisUser.getFullName)
				createUnprotectedCell(workbook, row, 2) // unprotect cell for the dropdown group name
			} else {
				createUnprotectedCell(workbook, row, 0) // cell for student_id
				createUnprotectedCell(workbook, row, 1) // cell for name (for the user's info only)
				createUnprotectedCell(workbook, row, 2) // cell for the group name
			}

			row.createCell(3).setCellFormula(
				"IF(ISTEXT($C"+(row.getRowNum + 1) + "), VLOOKUP($C" + (row.getRowNum + 1) + ", " + groupLookupRange + ", 2, FALSE), \" \")"
			)
		}

		formatWorkbook(workbook)
		workbook
	}


	def createUnprotectedCell(workbook: XSSFWorkbook, row: XSSFRow, col: Int, value: String = "") {
		val lockedCellStyle = workbook.createCellStyle();
		lockedCellStyle.setLocked(false);
		val cell = row.createCell(col)
		cell.setCellValue(value)
		cell.setCellStyle(lockedCellStyle)
	}



	// attaches the data validation to the sheet
	def generateGroupDropdowns(sheet: XSSFSheet, groups: List[SmallGroup]) {
		val dropdownChoices = groups.map(_.name).toArray
		val dropdownRange = new CellRangeAddressList(1, spreadsheetRows, 2, 2)
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

		groupSheet.protectSheet(sheetPassword)
	}


	def generateAllocationSheet(workbook: XSSFWorkbook): XSSFSheet =  {
		val sheet = workbook.createSheet(allocateSheetName)

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