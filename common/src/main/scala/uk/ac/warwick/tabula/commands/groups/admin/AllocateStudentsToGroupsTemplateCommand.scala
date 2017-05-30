package uk.ac.warwick.tabula.commands.groups.admin

import org.apache.poi.ss.usermodel.{Cell, DataValidation, Row, Sheet}
import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.model.{Member, Module, StudentMember}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.views.ExcelView
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object AllocateStudentsToGroupsTemplateCommand {
	def apply(module: Module, set: SmallGroupSet) =
		new AllocateStudentsToGroupsTemplateCommandInternal(module, set)
			with AutowiringProfileServiceComponent
			with ComposableCommand[ExcelView]
			with AllocateStudentsToGroupsTemplatePermissions
			with ReadOnly with Unaudited
}

class AllocateStudentsToGroupsTemplateCommandInternal(val module: Module, val set: SmallGroupSet)
	extends CommandInternal[ExcelView] with AllocateStudentsToGroupsTemplateCommandState {

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
		val setUsers = removePermanentlyWithdrawn(set.allStudents)
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

	def removePermanentlyWithdrawn(users: Seq[User]): Seq[User] = {
		val members: Seq[Member] = users.flatMap(usr => profileService.getMemberByUser(usr))
		val membersFiltered: Seq[Member] = members.filter {
			case (student: StudentMember) => !student.permanentlyWithdrawn
			case (member: Member) => true
		}
		membersFiltered.map { mem => mem.asSsoUser}
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

trait AllocateStudentsToGroupsTemplateCommandState {
	def module: Module
	def set: SmallGroupSet
}

trait AllocateStudentsToGroupsTemplatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AllocateStudentsToGroupsTemplateCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, module)
		p.PermissionCheck(Permissions.SmallGroups.Allocate, mandatory(set))
	}
}