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
	val AcceptedFileExtensions = Seq(".xlsx")
}

@Service
class RawStudentRelationshipExtractor {
	
	type RowData = Map[String, String]
	type RawStudentRelationship = (Member, Option[Member])
	type ErrorCode = (String, String)
	
	type ParsedRow = (RowData, Option[RawStudentRelationship], Seq[ErrorCode])
	
	var profileService = Wire[ProfileService]

	/**
	 * Method for reading in a xlsx spreadsheet and converting it into a list of relationships
	 */
	def readXSSFExcelFile(file: InputStream, relationshipType: StudentRelationshipType): Seq[ParsedRow] = {
		def parseStudent(row: RowData): (Option[StudentMember], Option[ErrorCode]) = {
			row("student_id") match {
				case strStudentId if strStudentId.matches("\\d+") =>
					val studentId = UniversityId.zeroPad(strStudentId)
					
					profileService.getMemberByUniversityId(studentId) match {
						case Some(student: StudentMember) =>
							student.mostSignificantCourseDetails match {
								case Some(scd) if scd.department == null => 
									(Some(student), Some("student_id" -> "profiles.relationship.allocate.student.noDepartment"))
								case Some(scd) if relationshipType.readOnly(scd.department) =>
									(Some(student), Some("student_id" -> "profiles.relationship.allocate.student.readOnlyDepartment"))
								case Some(scd) => (Some(student), None)
								case None => (Some(student), Some("student_id" -> "profiles.relationship.allocate.student.noCourseDetails"))
							}
						case Some(member) => (None, Some("student_id" -> "profiles.relationship.allocate.universityId.notStudent"))
						case _ => (None, Some("student_id" -> "profiles.relationship.allocate.universityId.notMember"))
					}
				case _ => (None, Some("student_id" -> "profiles.relationship.allocate.universityId.badFormat"))
			}
		}
		
		def parseAgent(row: RowData): (Option[Member], Option[ErrorCode]) = {
			row.get("agent_id") match {
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
		
		SpreadsheetHelpers.parseXSSFExcelFile(file)
			.filter { row => row.contains("student_id") && row("student_id").hasText } // Only if there is a student ID in the row
			.map { row =>
				val (student, studentError) = parseStudent(row)
				val (agent, agentError) = parseAgent(row)
				
				(row, student.map { student => student -> agent }, Seq(studentError, agentError).flatten)
			}
	}
}
