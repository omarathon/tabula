package uk.ac.warwick.tabula.profiles.services.docconversion

import java.io.ByteArrayInputStream
import scala.collection.JavaConverters._
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.eventusermodel.XSSFReader
import org.apache.poi.xssf.model.SharedStringsTable
import org.springframework.stereotype.Service
import org.xml.sax.InputSource
import org.xml.sax.helpers.XMLReaderFactory
import uk.ac.warwick.tabula.JavaImports._
import java.io.FileInputStream
import java.io.InputStream
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler
import org.apache.poi.xssf.model.StylesTable
import uk.ac.warwick.tabula.helpers.Logging
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler
import org.apache.poi.ss.util.CellReference
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.helpers.SpreadsheetHelpers
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

object RawStudentRelationshipExtractor {
	type RowData = Map[String, String]
	type RawStudentRelationship = (Member, Option[Member])
	type ErrorCode = (String, String)
	
	type ParsedRow = (RowData, Option[RawStudentRelationship], Seq[ErrorCode])
	
	val AcceptedFileExtensions = Seq(".xlsx")
}

class RawStudentRelationshipRow(relationshipType: StudentRelationshipType, val rowData: Map[String, String]) {
	import RawStudentRelationshipExtractor._
	
	var profileService = Wire[ProfileService]
	
	def extractStudent(): (Option[StudentMember], Option[ErrorCode]) = {
		def validateCourseDetails(student: StudentMember): Option[ErrorCode] = { 
			student.mostSignificantCourseDetails match {
				case Some(scd) if scd.department == null => 
					Some("student_id" -> "profiles.relationship.allocate.student.noDepartment")
				case Some(scd) if relationshipType.readOnly(scd.department) =>
					Some("student_id" -> "profiles.relationship.allocate.student.readOnlyDepartment")
				case Some(scd) => None
				case None => Some("student_id" -> "profiles.relationship.allocate.student.noCourseDetails")
			}
		}
		
		rowData("student_id") match {
			case strStudentId if strStudentId.matches("\\d+") =>
				val studentId = UniversityId.zeroPad(strStudentId)
				
				profileService.getMemberByUniversityId(studentId) match {
					case Some(student: StudentMember) => 
						(Some(student), validateCourseDetails(student))
						
					case Some(member) => // non-student member 
						(None, Some("student_id" -> "profiles.relationship.allocate.universityId.notStudent"))
						
					case _ => (None, Some("student_id" -> "profiles.relationship.allocate.universityId.notMember"))
				}
			case _ => (None, Some("student_id" -> "profiles.relationship.allocate.universityId.badFormat"))
		}
	}
	
	def extractAgent(): (Option[Member], Option[ErrorCode]) = {
		rowData.get("agent_id") match {
			case Some(strAgentId) if strAgentId.hasText && strAgentId.matches("\\d+") =>
				val agentId = UniversityId.zeroPad(strAgentId)
				
				profileService.getMemberByUniversityId(agentId) match {
					case Some(member) => (Some(member), None)
					case _ => (None, Some("agent_id" -> "profiles.relationship.allocate.universityId.notMember"))
				}
			case Some("ERROR:#N/A") | None => (None, None)
			case _ => (None, Some("agent_id" -> "profiles.relationship.allocate.universityId.badFormat"))
		}
	}
	
	// Only if there is a student ID in the row
	def isValid = rowData.contains("student_id") && rowData("student_id").hasText
}

@Service
class RawStudentRelationshipExtractor {
	import RawStudentRelationshipExtractor._

	/**
	 * Method for reading in a xlsx spreadsheet and converting it into a list of relationships
	 */
	def readXSSFExcelFile(file: InputStream, relationshipType: StudentRelationshipType): Seq[ParsedRow] =
		SpreadsheetHelpers.parseXSSFExcelFile(file)
			.map { rowData => new RawStudentRelationshipRow(relationshipType, rowData) }
			.filter { _.isValid } // Ignore blank rows
			.map { row =>
				val (student, studentError) = row.extractStudent()
				val (agent, agentError) = row.extractAgent()
				
				(row.rowData, student.map { student => (student -> agent) }, Seq(studentError, agentError).flatten)
			}
}
