package uk.ac.warwick.tabula.services.cm2.docconversion

import java.io.InputStream

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.markingworkflow.CM2MarkingWorkflow
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.{FoundUser, SpreadsheetHelpers}
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.User
import MarkerAllocationExtractor._
import uk.ac.warwick.tabula.commands.cm2.assignments.AssignMarkersTemplateCommand

object MarkerAllocationExtractor {
	case class Error(field: String, rowData: Map[String, String], code: String, codeArgument: Array[Object] = Array())
	val AcceptedFileExtensions = Seq(".xlsx")

	case class ParsedRow (
		marker: Option[User],
		student: Option[User],
		role: Option[String],
		errors: Seq[Error]
	)
}

@Service
class MarkerAllocationExtractor {

	import AssignMarkersTemplateCommand._

	@transient var userLookup: UserLookupService = Wire[UserLookupService]

	def extractMarkersFromSpreadsheet(file: InputStream, workflow: CM2MarkingWorkflow): Map[String, Seq[ParsedRow]] = {

		val sheets = SpreadsheetHelpers.parseXSSFExcelFileWithSheetMetadata(file, simpleHeaders = false)
		val allMarkers = workflow.allMarkers

		val findMarker = {
			val markers = if(workflow.workflowType.rolesShareAllocations) {
				workflow.markersByRole
			} else {
				workflow.markers.map{case (s, m) => s.name -> m }
			}
			(s: String) => markers.get(s)
		}

		val roles = if (workflow.workflowType.rolesShareAllocations) {
			workflow.allStages.groupBy(_.roleName).keys.toSeq
		} else {
			workflow.allStages.map(_.allocationName)
		}

		def parseRow(role:Option[String], rowData: Map[String, String]): ParsedRow = {

			def getUser(id: String): Option[User] = Option(userLookup.getUserByUserId(id)).filter(_.isFoundUser)

			val student: Either[Error, User] = rowData.get(StudentUsercode) match {
				case Some(studentId)  => getUser(studentId)
					.map(Right(_))
					.getOrElse(Left(Error(StudentUsercode, rowData, "workflow.allocateMarkers.universityId.notFound")))
				case None => Left(Error(StudentUsercode, rowData, "workflow.allocateMarkers.usercode.missing"))
			}

			val validMarkers = role.flatMap(findMarker).getOrElse(Nil)

			val marker: Either[Error, User] = rowData.get(MarkerUsercode).filter(_.hasText).map(markerId => {
				validMarkers.find(user =>
					user.getUserId == markerId && getUser(markerId).exists(_.getUserId == user.getUserId)
				) match {
					case Some(FoundUser(user)) => Right(user)
					case _ if validMarkers.exists(_.getUserId == markerId) =>
						Left(Error(MarkerUsercode, rowData, "workflow.allocateMarkers.nonPrimary",
							Array(
								validMarkers.find(_.getUserId == markerId).get.getUserId, // usercode in marking workflow
								getUser(markerId).get.getUserId // actual primary usercode
							)
						))
					case _ if role.isEmpty =>
						Left(Error(MarkerUsercode, rowData, "workflow.allocateMarkers.unableToWorkOutRole"))
					case _ if allMarkers.exists(_.getUserId == markerId) =>
						Left(Error(MarkerUsercode, rowData, "workflow.allocateMarkers.wrongRole", Array(role.get)))
					case _ if getUser(markerId).nonEmpty =>
						Left(Error(MarkerUsercode, rowData, "workflow.allocateMarkers.notMarker"))
					case _ =>
						Left(Error(MarkerUsercode, rowData, "workflow.allocateMarkers.universityId.notFound"))
				}
			}).getOrElse(Left(Error(MarkerUsercode, rowData, "workflow.allocateMarkers.universityId.notFound")))


			val errors = Seq(student, marker).flatMap(_.left.toOption)
			ParsedRow(marker.right.toOption, student.right.toOption, role, errors)
		}

		val parsedRows: Seq[ParsedRow] = for(sheet <- sheets; row <- sheet.rows)
			yield parseRow(roles.find(_ == sheet.name), row)

		parsedRows.groupBy(_.role.getOrElse("No role"))
	}

}

trait MarkerAllocationExtractorComponent {
	def markerAllocationExtractor: MarkerAllocationExtractor
}

trait AutowiringMarkerAllocationExtractorComponent extends MarkerAllocationExtractorComponent {
	var markerAllocationExtractor: MarkerAllocationExtractor = Wire[MarkerAllocationExtractor]
}
