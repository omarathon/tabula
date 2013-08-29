package uk.ac.warwick.tabula.profiles.commands.tutor

import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.views.ExcelView
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.data.model.RelationshipType
import uk.ac.warwick.tabula.services.ProfileService
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFRow
import uk.ac.warwick.tabula.data.model.Member
import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper
import org.apache.poi.xssf.usermodel.XSSFDataValidationConstraint
import org.apache.poi.xssf.usermodel.XSSFDataValidation
import scala.Option.option2Iterable

class TutorTemplateCommand(val department: Department) extends Command[ExcelView] with ReadOnly with Unaudited {

	PermissionCheck(Permissions.Profiles.PersonalTutor.Upload, department)
	
	var service = Wire[RelationshipService]
	var profileService = Wire[ProfileService]
	
	val tutorLookupSheetName = "TutorLookup"
	val allocateSheetName = "Tutors for " + safeDepartmentName(department)
	val sheetPassword = "roygbiv"

	def applyInternal() = {
		val workbook = generateWorkbook()

		new ExcelView("Allocation for " + safeDepartmentName(department) + " tutors.xlsx", workbook)
	}
	
	def generateWorkbook() = {
		val existingRelationships = service.listStudentRelationshipsByDepartment(RelationshipType.PersonalTutor, department)
		val unallocated = service.listStudentsWithoutRelationship(RelationshipType.PersonalTutor, department)
		
		val allTutors = 
			existingRelationships
				.groupBy(_.agent)
				.filter { case (agent, _) => agent.forall(_.isDigit) }
				.flatMap { case (agent, _) => profileService.getMemberByUniversityId(agent)	}
				.toSeq
				
		// Transform into a list of (Member, Seq[Member]) pairs
		val existingAllocations = 
			existingRelationships
				.groupBy(_.targetSprCode)
				.toSeq
				.flatMap { case (sprCode, rels) => 
					val student = profileService.getStudentBySprCode(sprCode)
					val tutors = rels.flatMap { rel => profileService.getMemberByUniversityId(rel.agent) }
					
					(student, tutors) match {
						case (None, _) => None
						case (_, Nil) => None
						case (Some(student), tutors) => Some((student, tutors))
					}
				}
				
		val allAllocations = 
			(existingAllocations ++ (unallocated.map { (_, Nil) }))
			.sortBy { case (student, _) => student.lastName + ", " + student.firstName }

		val workbook = new XSSFWorkbook()
		val sheet: XSSFSheet = generateAllocationSheet(workbook)
		generateTutorLookupSheet(workbook, allTutors)
		generateTutorDropdowns(sheet, allTutors)

		val tutorLookupRange = tutorLookupSheetName + "!$A2:$B" + (allTutors.length + 1)
		val allocationIterator = allAllocations.iterator

		allAllocations.foreach { case (student, tutors) => 
			val row = sheet.createRow(sheet.getLastRowNum + 1)

			row.createCell(0).setCellValue(student.universityId)
			row.createCell(1).setCellValue(student.fullName.getOrElse(""))
			
			val tutorNameCell = createUnprotectedCell(workbook, row, 2) // unprotect cell for the dropdown tutor name
			tutors.headOption.flatMap { _.fullName }.foreach(tutorNameCell.setCellValue(_))
			
			row.createCell(3).setCellFormula(
				"IF(ISTEXT($C" + (row.getRowNum + 1) + "), VLOOKUP($C" + (row.getRowNum + 1) + ", " + tutorLookupRange + ", 2, FALSE), \" \")"
			)
		}

		formatWorkbook(workbook)
		workbook
	}
	
	def generateTutorLookupSheet(workbook: XSSFWorkbook, tutors: Seq[Member]) {
		val tutorSheet: XSSFSheet = workbook.createSheet(tutorLookupSheetName)

		for (tutor <- tutors) {
			val row = tutorSheet.createRow(tutorSheet.getLastRowNum() + 1)
			row.createCell(0).setCellValue(tutor.fullName.getOrElse(tutor.universityId))
			row.createCell(1).setCellValue(tutor.universityId)
		}

		tutorSheet.protectSheet(sheetPassword)
	}
	
	// attaches the data validation to the sheet
	def generateTutorDropdowns(sheet: XSSFSheet, tutors: Seq[Member]) {
		val dropdownChoices = tutors.flatMap(_.fullName).toArray
		val dropdownRange = new CellRangeAddressList(1, tutors.length, 2, 2)
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

	def createUnprotectedCell(workbook: XSSFWorkbook, row: XSSFRow, col: Int, value: String = "") = {
		val lockedCellStyle = workbook.createCellStyle();
		lockedCellStyle.setLocked(false);
		val cell = row.createCell(col)
		cell.setCellValue(value)
		cell.setCellStyle(lockedCellStyle)
		cell
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
	
	def generateAllocationSheet(workbook: XSSFWorkbook): XSSFSheet =  {
		val sheet = workbook.createSheet(allocateSheetName)

		// add header row
		val header = sheet.createRow(0)
		header.createCell(0).setCellValue("student_id")
		header.createCell(1).setCellValue("Student name")
		header.createCell(2).setCellValue("Tutor name")
		header.createCell(3).setCellValue("tutor_id")

		// using apache-poi, we can't protect certain cells - rather we have to protect
		// the entire sheet and then unprotect the ones we want to remain editable
		sheet.protectSheet(sheetPassword)
		sheet
	}
	
	// trim the name down to 21 characters. Excel sheet names must be 31 chars or less so
	// "Marks for " = 10 chars + assignment name (max 21) = 31
	def trimmedDepartmentName(department: Department) = {
		if (department.name.length > 21)
			department.name.substring(0, 21)
		else
			department.name
	}

	// util to replace unsafe characters with spaces
	def safeDepartmentName(department: Department) = WorkbookUtil.createSafeSheetName(trimmedDepartmentName(department))
}