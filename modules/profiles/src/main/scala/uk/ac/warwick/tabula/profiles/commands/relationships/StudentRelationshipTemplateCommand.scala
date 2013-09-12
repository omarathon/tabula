package uk.ac.warwick.tabula.profiles.commands.relationships

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
import uk.ac.warwick.tabula.services.ProfileService
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFRow
import uk.ac.warwick.tabula.data.model.Member
import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper
import org.apache.poi.xssf.usermodel.XSSFDataValidationConstraint
import org.apache.poi.xssf.usermodel.XSSFDataValidation
import scala.Option.option2Iterable
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

class StudentRelationshipTemplateCommand(val department: Department, val relationshipType: StudentRelationshipType) extends Command[ExcelView] with ReadOnly with Unaudited {

	PermissionCheck(Permissions.Profiles.StudentRelationship.Read(relationshipType), department)
	
	var service = Wire[RelationshipService]
	var profileService = Wire[ProfileService]
	
	val agentLookupSheetName = "AgentLookup"
	val allocateSheetName = trimmedSheetName(relationshipType.agentRole.capitalize + "s for " + department.name)
	val sheetPassword = "roygbiv"

	def applyInternal() = {
		val workbook = generateWorkbook()

		new ExcelView("Allocation for " + allocateSheetName + ".xlsx", workbook)
	}
	
	def generateWorkbook() = {
		val existingRelationships = service.listStudentRelationshipsByDepartment(relationshipType, department)
		val unallocated = service.listStudentsWithoutRelationship(relationshipType, department)
		
		val allAgents = 
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
					val agents = rels.flatMap { rel => profileService.getMemberByUniversityId(rel.agent) }
					
					(student, agents) match {
						case (None, _) => None
						case (_, Nil) => None
						case (Some(student), agents) => Some((student, agents))
					}
				}
				
		val allAllocations = 
			(existingAllocations ++ (unallocated.map { (_, Nil) }))
			.sortBy { case (student, _) => student.lastName + ", " + student.firstName }

		val workbook = new XSSFWorkbook()
		val sheet: XSSFSheet = generateAllocationSheet(workbook)
		val agentSheet = generateAgentLookupSheet(workbook, allAgents)
		generateAgentDropdowns(sheet, allAgents, allAllocations)

		val agentLookupRange = agentLookupSheetName + "!$A2:$B" + (allAgents.length + 1)
		val allocationIterator = allAllocations.iterator

		allAllocations.foreach { case (student, agents) => 
			val row = sheet.createRow(sheet.getLastRowNum + 1)

			row.createCell(0).setCellValue(student.universityId)
			row.createCell(1).setCellValue(student.fullName.getOrElse(""))
			
			val agentNameCell = createUnprotectedCell(workbook, row, 2) // unprotect cell for the dropdown agent name
			agents.headOption.flatMap { _.fullName }.foreach(agentNameCell.setCellValue(_))
			
			row.createCell(3).setCellFormula(
				"IF(ISTEXT($C" + (row.getRowNum + 1) + "), VLOOKUP($C" + (row.getRowNum + 1) + ", " + agentLookupRange + ", 2, FALSE), \" \")"
			)
		}

		formatWorkbook(workbook)
		workbook
	}
	
	def generateAgentLookupSheet(workbook: XSSFWorkbook, agents: Seq[Member]) = {
		val agentSheet: XSSFSheet = workbook.createSheet(agentLookupSheetName)

		for (agent <- agents) {
			val row = agentSheet.createRow(agentSheet.getLastRowNum() + 1)
			row.createCell(0).setCellValue(agent.fullName.getOrElse(agent.universityId))
			row.createCell(1).setCellValue(agent.universityId)
		}

		agentSheet.protectSheet(sheetPassword)
		agentSheet
	}
	
	// attaches the data validation to the sheet
	def generateAgentDropdowns(sheet: XSSFSheet, agents: Seq[Member], allAllocations: Seq[(uk.ac.warwick.tabula.data.model.Member, 
 Seq[uk.ac.warwick.tabula.data.model.Member])]) {
		if (!agents.isEmpty) {
			val dropdownChoices = agents.flatMap(_.fullName).toArray
			val dropdownRange = new CellRangeAddressList(1, allAllocations.length, 2, 2)
			val validation = getDataValidation(agents, sheet, dropdownRange)
	
			sheet.addValidationData(validation)
		}
	}
	
	// Excel data validation - will only accept the values fed to this method, also puts a dropdown on each cell
	def getDataValidation(agents: Seq[Member], sheet: XSSFSheet, addressList: CellRangeAddressList) = {
		val dvHelper = new XSSFDataValidationHelper(sheet)
		val dvConstraint = dvHelper.createFormulaListConstraint(agentLookupSheetName + "!$A2:$A" + (agents.length + 1))
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
		0 to 3 foreach  { col => 
			sheet.setDefaultColumnStyle(col, style)
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
		header.createCell(1).setCellValue(relationshipType.studentRole.capitalize + " name")
		header.createCell(2).setCellValue(relationshipType.agentRole.capitalize + " name")
		header.createCell(3).setCellValue("agent_id")

		// using apache-poi, we can't protect certain cells - rather we have to protect
		// the entire sheet and then unprotect the ones we want to remain editable
		sheet.protectSheet(sheetPassword)
		sheet
	}
	
	// Excel sheet names must be 31 chars or less so
	def trimmedSheetName(rawSheetName: String) = {
		val sheetName = WorkbookUtil.createSafeSheetName(rawSheetName)
		
		if (sheetName.length > 31) sheetName.substring(0, 31)
		else sheetName
	}
}
