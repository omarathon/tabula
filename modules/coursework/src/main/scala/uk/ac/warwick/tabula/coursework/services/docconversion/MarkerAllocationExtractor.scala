package uk.ac.warwick.tabula.coursework.services.docconversion

import java.io.InputStream

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.coursework.services.docconversion.MarkerAllocationExtractor._
import uk.ac.warwick.tabula.data.model.MarkingWorkflow
import uk.ac.warwick.tabula.helpers.SpreadsheetHelpers
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.User

object MarkerAllocationExtractor {
	case class Error(field:String, rowData: Map[String, String], code:String)
	val AcceptedFileExtensions = Seq(".xlsx")

	sealed trait MarkerPosition
	case object FirstMarker extends MarkerPosition
	case object SecondMarker extends MarkerPosition
	case object NoMarker extends MarkerPosition

	case class ParsedRow (
		marker: Option[User],
		student: Option[User],
		errors: Seq[Error],
		position: MarkerPosition
	)
}

@Service
class MarkerAllocationExtractor() {

	@transient var userLookup = Wire[UserLookupService]

	def extractMarkersFromSpreadsheet(file: InputStream, workflow: MarkingWorkflow) = {
		val rowData = SpreadsheetHelpers.parseXSSFExcelFile(file)
		rowData
			.map(parseRow(_, workflow))
			.groupBy(_.position)
	}

	def parseRow(rowData: Map[String, String], workflow: MarkingWorkflow): ParsedRow = {

		lazy val allMarkers = workflow.firstMarkers.knownType.members ++  workflow.secondMarkers.knownType.members

		def getUser(id: String): Option[User] = {
			val user = userLookup.getUserByWarwickUniId(id)
			if (user.isFoundUser) Some(user)
			else None
		}

		def parseStudent: Either[Error, User] = {
			rowData.get("student_id") match {
				case Some(studentId) if studentId.matches("\\d+") =>
					getUser(studentId)
						.map(Right(_))
						.getOrElse(Left(Error("student_id", rowData, "workflow.allocateMarkers.universityId.notFound")))
				case Some(studentId) => Left(Error("student_id", rowData,"workflow.allocateMarkers.universityId.badFormat"))
				case None => Left(Error("student_id", rowData, "workflow.allocateMarkers.universityId.missing"))
			}
		}

		def parseMarker: Either[MarkerAllocationExtractor.Error, Option[User]] = {
			rowData.get("agent_id") match {
				case Some(markerId) if markerId.hasText =>
					if (!allMarkers.contains(markerId)) {
						Left(Error("marker_id", rowData, "workflow.allocateMarkers.notMarker"))
					} else {
						getUser(markerId)
							.map(user => Right(Some(user)))
							.getOrElse(Left(Error("marker_id", rowData, "workflow.allocateMarkers.universityId.notFound")))
					}
				case _ =>
					Left(Error("marker_id", rowData, "workflow.allocateMarkers.universityId.notFound"))
			}
		}

		def parsePosition = {
			def sanitiseRoleName(roleName: String) = roleName.toLowerCase.replace(" ","_")
			if (rowData.contains(s"${sanitiseRoleName(workflow.firstMarkerRoleName)}_name")) FirstMarker
			else if (rowData.contains(s"${workflow.secondMarkerRoleName.map(sanitiseRoleName).getOrElse("")}_name")) SecondMarker
			else NoMarker
		}

		val student = parseStudent
		val marker = parseMarker
		val errors = Seq(student, marker).flatMap(_.left.toOption)
		ParsedRow(marker.right.toOption.flatten, student.right.toOption, errors, parsePosition)
	}
}

