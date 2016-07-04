package uk.ac.warwick.tabula.commands.groups.admin

import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.xssf.usermodel._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.admin.SmallGroupSetsSpreadsheetTemplateCommand._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, SmallGroupAllocationMethod, SmallGroupFormat, SmallGroupSet}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.groups.docconversion.{ExtractedSmallGroup, ExtractedSmallGroupEvent, ExtractedSmallGroupSet}
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.views.ExcelView

import scala.collection.JavaConverters._

object SmallGroupSetsSpreadsheetTemplateCommand {
	val RequiredPermission = Permissions.SmallGroups.ImportFromExternalSystem
	type CommandType = Appliable[ExcelView]

	def apply(department: Department, academicYear: AcademicYear): CommandType =
		new SmallGroupSetsSpreadsheetTemplateCommandInternal(department, academicYear)
			with ComposableCommand[ExcelView]
			with SmallGroupSetsSpreadsheetTemplatePermissions
			with AutowiringSmallGroupServiceComponent
			with ReadOnly with Unaudited
}

abstract class SmallGroupSetsSpreadsheetTemplateCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[ExcelView] with SmallGroupSetsSpreadsheetTemplateState {
	self: SmallGroupServiceComponent =>

	override def applyInternal(): ExcelView = {
		val workbook = generateWorkbook()
		new ExcelView(s"Small groups for ${department.name} - ${academicYear.startYear}-${academicYear.endYear}.xlsx", workbook)
	}

	// Publicly visible for testing
	def generateWorkbook() = {
		val workbook = new XSSFWorkbook()

		val sets = smallGroupService.getSmallGroupSets(department, academicYear).sorted

		addSetsSheet(workbook, sets)
		addGroupsSheet(workbook, sets)
		addEventsSheet(workbook, sets)

		addLookupsSheet(workbook)

		workbook
	}

	private def addLookupsSheet(workbook: XSSFWorkbook): XSSFSheet = {
		val style = workbook.createCellStyle
		val format = workbook.createDataFormat

		style.setDataFormat(format.getFormat("@"))
		style.setFont({
			val font = workbook.createFont()
			font.setFontHeightInPoints(11)
			font.setFontName("Calibri")
			font
		})

		val sheet = workbook.createSheet("Lookups")

		val header = sheet.createRow(0)
		header.createCell(0).setCellValue("Small group set types")
		header.createCell(1).setCellValue("Allocation methods")
		header.createCell(2).setCellValue("Days of the week")

		val rows = 1 to (math.max(SmallGroupFormat.members.size, math.max(SmallGroupAllocationMethod.members.size, DayOfWeek.members.size)) + 1) map sheet.createRow

		SmallGroupFormat.members.zipWithIndex.foreach { case (f, index) =>
			val row = rows(index)
			row.createCell(0).setCellValue(f.description)
		}

		SmallGroupAllocationMethod.members.zipWithIndex.foreach { case (am, index) =>
			val row = rows(index)
			row.createCell(1).setCellValue(am.description)
		}

		DayOfWeek.members.zipWithIndex.foreach { case (day, index) =>
			val row = rows(index)
			row.createCell(2).setCellValue(day.name)
		}

		// set style on all columns
		0 to 2 foreach  { col =>
			sheet.setDefaultColumnStyle(col, style)
			sheet.autoSizeColumn(col)
		}

		header.setRowStyle({
			val style = workbook.createCellStyle()
			val font = workbook.createFont()
			font.setBold(true)
			style.setFont(font)
			style
		})

		sheet
	}

	private def addSetsSheet(workbook: XSSFWorkbook, sets: Seq[SmallGroupSet]): XSSFSheet = {
		val style = workbook.createCellStyle
		val format = workbook.createDataFormat

		style.setDataFormat(format.getFormat("@"))
		style.setFont({
			val font = workbook.createFont()
			font.setFontHeightInPoints(11)
			font.setFontName("Calibri")
			font
		})

		val sheet = workbook.createSheet("Sets")

		val header = sheet.createRow(0)

		// set style on all columns
		ExtractedSmallGroupSet.AllColumns.zipWithIndex foreach  { case (column, index) =>
			header.createCell(index).setCellValue(column)
		}

		sets.zipWithIndex.foreach { case (set, index) =>
			val row = sheet.createRow(index + 1)
			row.createCell(0).setCellValue(set.module.code.toUpperCase)
			row.createCell(1).setCellValue(set.format.description)
			row.createCell(2).setCellValue(set.name)
			row.createCell(3).setCellValue(set.allocationMethod.description)
			row.createCell(4).setCellValue(set.studentsCanSeeTutorName)
			row.createCell(5).setCellValue(set.studentsCanSeeOtherMembers)
			row.createCell(6).setCellValue(set.allowSelfGroupSwitching)
			row.createCell(7).setCellValue(Option(set.linkedDepartmentSmallGroupSet).map(_.name).orNull)
			row.createCell(8).setCellValue(set.collectAttendance)
		}

		if (sets.nonEmpty) {
			// Small group format validation
			{
				val dropdownRange = new CellRangeAddressList(1, sheet.getLastRowNum, 1, 1)
				val dvHelper = new XSSFDataValidationHelper(sheet)
				val dvConstraint =
					dvHelper.createFormulaListConstraint("Lookups!$A$2:$A$" + (SmallGroupFormat.members.size + 1))
						.asInstanceOf[XSSFDataValidationConstraint]
				val validation = dvHelper.createValidation(dvConstraint, dropdownRange).asInstanceOf[XSSFDataValidation]
				validation.setShowErrorBox(true)
				sheet.addValidationData(validation)
			}

			// Small group allocation method validation
			{
				val dropdownRange = new CellRangeAddressList(1, sheet.getLastRowNum, 3, 3)
				val dvHelper = new XSSFDataValidationHelper(sheet)
				val dvConstraint =
					dvHelper.createFormulaListConstraint("Lookups!$B$2:$B$" + (SmallGroupAllocationMethod.members.size + 1))
						.asInstanceOf[XSSFDataValidationConstraint]
				val validation = dvHelper.createValidation(dvConstraint, dropdownRange).asInstanceOf[XSSFDataValidation]
				validation.setShowErrorBox(true)
				sheet.addValidationData(validation)
			}
		}

		// set style on all columns
		0 to ExtractedSmallGroupSet.AllColumns.size foreach  { index =>
			sheet.setDefaultColumnStyle(index, style)
			sheet.autoSizeColumn(index)
		}

		// Boolean columns
		val booleanStyle = workbook.createCellStyle
		booleanStyle.cloneStyleFrom(style)
		booleanStyle.setDataFormat(format.getFormat("BOOLEAN"))
		sheet.setDefaultColumnStyle(4, booleanStyle)
		sheet.setDefaultColumnStyle(5, booleanStyle)
		sheet.setDefaultColumnStyle(6, booleanStyle)
		sheet.setDefaultColumnStyle(8, booleanStyle)

		header.setRowStyle({
			val style = workbook.createCellStyle()
			val font = workbook.createFont()
			font.setBold(true)
			style.setFont(font)
			style
		})

		sheet
	}

	private def addGroupsSheet(workbook: XSSFWorkbook, sets: Seq[SmallGroupSet]): XSSFSheet = {
		val style = workbook.createCellStyle
		val format = workbook.createDataFormat

		style.setDataFormat(format.getFormat("@"))
		style.setFont({
			val font = workbook.createFont()
			font.setFontHeightInPoints(11)
			font.setFontName("Calibri")
			font
		})

		val sheet = workbook.createSheet("Groups")

		val header = sheet.createRow(0)

		// set style on all columns
		ExtractedSmallGroup.AllColumns.zipWithIndex foreach  { case (column, index) =>
			header.createCell(index).setCellValue(column)
		}

		sets.flatMap(_.groups.asScala.sorted).zipWithIndex.foreach { case (group, index) =>
			val row = sheet.createRow(index + 1)
			row.createCell(0).setCellValue(group.groupSet.module.code.toUpperCase)
			row.createCell(1).setCellValue(group.groupSet.name)
			row.createCell(2).setCellValue(group.name)
			row.createCell(3)

			if (group.maxGroupSize != null) row.getCell(3).setCellValue(group.maxGroupSize.toInt)
		}

		// set style on all columns
		0 to ExtractedSmallGroup.AllColumns.size foreach  { index =>
			sheet.setDefaultColumnStyle(index, style)
			sheet.autoSizeColumn(index)
		}

		// Limit column
		val intStyle = workbook.createCellStyle
		intStyle.cloneStyleFrom(style)
		intStyle.setDataFormat(format.getFormat("0"))
		sheet.setDefaultColumnStyle(3, intStyle)

		header.setRowStyle({
			val style = workbook.createCellStyle()
			val font = workbook.createFont()
			font.setBold(true)
			style.setFont(font)
			style
		})

		sheet
	}

	private def addEventsSheet(workbook: XSSFWorkbook, sets: Seq[SmallGroupSet]): XSSFSheet = {
		val style = workbook.createCellStyle
		val format = workbook.createDataFormat

		style.setDataFormat(format.getFormat("@"))
		style.setFont({
			val font = workbook.createFont()
			font.setFontHeightInPoints(11)
			font.setFontName("Calibri")
			font
		})

		val sheet = workbook.createSheet("Events")

		val header = sheet.createRow(0)

		// set style on all columns
		ExtractedSmallGroupEvent.AllColumns.zipWithIndex foreach  { case (column, index) =>
			header.createCell(index).setCellValue(column)
		}

		sets.flatMap(_.groups.asScala.sorted).flatMap(_.events.sorted).zipWithIndex.foreach { case (event, index) =>
			val row = sheet.createRow(index + 1)
			row.createCell(0).setCellValue(event.group.groupSet.module.code.toUpperCase)
			row.createCell(1).setCellValue(event.group.groupSet.name)
			row.createCell(2).setCellValue(event.group.name)
			row.createCell(3).setCellValue(event.title)
			row.createCell(4).setCellValue(event.tutors.knownType.members.mkString(","))
			row.createCell(5).setCellValue(event.weekRanges.map(_.toString).mkString(","))
			row.createCell(6).setCellValue(event.day.name)
			row.createCell(7).setCellValue(DateUtil.convertTime(event.startTime.toString("HH:mm")))
			row.createCell(8).setCellValue(DateUtil.convertTime(event.endTime.toString("HH:mm")))
			row.createCell(9).setCellValue(event.location.name)
		}

		// Day of week validation
		if (sets.flatMap(_.groups.asScala.sorted).flatMap(_.events.sorted).nonEmpty) {
			val dropdownRange = new CellRangeAddressList(1, sheet.getLastRowNum, 6, 6)
			val dvHelper = new XSSFDataValidationHelper(sheet)
			val dvConstraint =
				dvHelper.createFormulaListConstraint("Lookups!$C$2:$C$" + (DayOfWeek.members.size + 1))
					.asInstanceOf[XSSFDataValidationConstraint]
			val validation = dvHelper.createValidation(dvConstraint, dropdownRange).asInstanceOf[XSSFDataValidation]
			validation.setShowErrorBox(true)
			sheet.addValidationData(validation)
		}

		// set style on all columns
		0 to ExtractedSmallGroupEvent.AllColumns.size foreach  { index =>
			sheet.setDefaultColumnStyle(index, style)
			sheet.autoSizeColumn(index)
		}

		// Time columns
		val timeStyle = workbook.createCellStyle
		timeStyle.cloneStyleFrom(style)
		timeStyle.setDataFormat(format.getFormat("HH:MM"))
		sheet.setDefaultColumnStyle(7, timeStyle)
		sheet.setDefaultColumnStyle(8, timeStyle)

		header.setRowStyle({
			val style = workbook.createCellStyle()
			val font = workbook.createFont()
			font.setBold(true)
			style.setFont(font)
			style
		})

		sheet
	}

}

trait SmallGroupSetsSpreadsheetTemplateState {
	def department: Department
	def academicYear: AcademicYear
}

trait SmallGroupSetsSpreadsheetTemplatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: SmallGroupSetsSpreadsheetTemplateState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = p.PermissionCheck(RequiredPermission, mandatory(department))
}