package uk.ac.warwick.tabula.coursework.commands.assignments

import org.springframework.validation.BindingResult
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.coursework.services.docconversion.MarkerAllocationExtractor
import uk.ac.warwick.tabula.coursework.services.docconversion.MarkerAllocationExtractor.{NoMarker, SecondMarker, FirstMarker, ParsedRow}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{AutowiringAssessmentServiceComponent, AssessmentServiceComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.{AutowiringUserGroupDaoComponent, UserGroupDaoComponent}


object AssignMarkersCommand {
	def apply(module: Module, assignment: Assignment) =
		new AssignMarkersCommand(module, assignment)
		with ComposableCommand[Assignment]
		with AssignMarkersPermission
		with AssignMarkersDescription
		with AssignMarkersCommandState
		with AutowiringAssessmentServiceComponent
		with AutowiringUserGroupDaoComponent
}

class AssignMarkersCommand(val module: Module, val assignment: Assignment)
	extends CommandInternal[Assignment] with BindListener {

	self: AssignMarkersCommandState with AssessmentServiceComponent with UserGroupDaoComponent =>

	var alloctaionExtractor = Wire[MarkerAllocationExtractor]
	var file: UploadedFile = new UploadedFile

	var firstMarkerMapping : JMap[String, JList[String]] = assignment.markingWorkflow.firstMarkers.knownType.members.map({ marker =>
		val list : JList[String] = JArrayList()
		(marker, list)
	}).toMap.asJava

	var secondMarkerMapping : JMap[String, JList[String]] = assignment.markingWorkflow.secondMarkers.knownType.members.map({ marker =>
		val list : JList[String] = JArrayList()
		(marker, list)
	}).toMap.asJava

	case class Allocation(marker:Option[User], students: Seq[User])
	@transient var sheetFirstMarkers : Seq[Allocation] = Nil
	@transient var sheetSecondMarkers : Seq[Allocation] = Nil
	@transient var sheetErrors : Seq[ParsedRow] = Nil
	@transient var unallocatedStudents : Seq[User] = Nil

	def applyInternal() = {

		if (assignment.firstMarkers != null) {
			assignment.firstMarkers.clear()
		} else {
			assignment.firstMarkers = JArrayList()
		}

		assignment.firstMarkers.addAll(firstMarkerMapping.asScala.map { case (markerId, studentIds) =>
			val group = UserGroup.ofUsercodes
			group.includedUserIds = studentIds.asScala
			userGroupDao.saveOrUpdate(group)
			FirstMarkersMap(assignment, markerId, group)
		}.toSeq.asJava)

		if (assignment.secondMarkers != null) {
			assignment.secondMarkers.clear()
		} else {
			assignment.secondMarkers = JArrayList()
		}

		assignment.secondMarkers.addAll(secondMarkerMapping.asScala.map { case (markerId, studentIds) =>
			val group = UserGroup.ofUsercodes
			group.includedUserIds = studentIds.asScala
			userGroupDao.saveOrUpdate(group)
			SecondMarkersMap(assignment, markerId, group)
		}.toSeq.asJava)

		assessmentService.save(assignment)
		assignment
	}

	def extractDataFromFile(file: FileAttachment, result: BindingResult) = {
		val rowData = alloctaionExtractor.extractMarkersFromSpreadsheet(file.dataStream, workflow)

		def rowsToMarkerMap(rows: Seq[ParsedRow]) = {
			rows
				.filter(_.errors.isEmpty)
				.groupBy(a => a.marker)
				.map{ case (marker, row) => Allocation(marker, row.flatMap(_.student))}
				.toSeq
		}


		sheetFirstMarkers = rowData.get(FirstMarker).map(rowsToMarkerMap).getOrElse(Nil)
		sheetSecondMarkers = rowData.get(SecondMarker).map(rowsToMarkerMap).getOrElse(Nil)
		sheetErrors = rowData.values.flatten.filterNot(_.errors.isEmpty).toSeq
		unallocatedStudents = rowData.get(NoMarker).getOrElse(Nil).filter(_.errors.isEmpty).flatMap(_.student)
	}

	def validateUploadedFile(result: BindingResult) {
		val fileNames = file.fileNames map (_.toLowerCase)
		val invalidFiles = fileNames.filter(s => !MarkerAllocationExtractor.AcceptedFileExtensions.exists(s.endsWith))

		if (invalidFiles.size > 0) {
			if (invalidFiles.size == 1) result.rejectValue("file", "file.wrongtype.one", Array(invalidFiles.mkString("")), "")
			else result.rejectValue("", "file.wrongtype", Array(invalidFiles.mkString(", ")), "")
		}
	}

	override def onBind(result: BindingResult) {
		validateUploadedFile(result)

		if (!result.hasErrors) {
			transactional() {
				file.onBind(result)
				if (!file.attached.isEmpty) {
					extractDataFromFile(file.attached.asScala.head, result)
				}
			}
		}
	}

}

trait AssignMarkersPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AssignMarkersCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Assignment.Update, assignment)
	}

}

trait AssignMarkersDescription extends Describable[Assignment] {

	self: AssignMarkersCommandState =>

	override lazy val eventName = "AssignMarkers"

	override def describe(d: Description) {
		d.assignment(assignment)
	}

}

trait AssignMarkersCommandState {
	def module: Module
	def assignment: Assignment
	def workflow = assignment.markingWorkflow
}
