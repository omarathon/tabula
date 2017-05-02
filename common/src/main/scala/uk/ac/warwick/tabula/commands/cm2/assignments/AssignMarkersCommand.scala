package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowService.Allocations
import uk.ac.warwick.tabula.services.cm2.docconversion.MarkerAllocationExtractor.ParsedRow
import uk.ac.warwick.tabula.services.cm2.docconversion.{AutowiringMarkerAllocationExtractorComponent, MarkerAllocationExtractorComponent}
import uk.ac.warwick.tabula.system.BindListener


import scala.collection.JavaConverters._

object AssignMarkersCommand {

	def apply(assignment: Assignment) =
		new AssignMarkersCommandInternal(assignment)
			with ComposableCommand[Assignment]
			with AssignMarkersDragAndDropState
			with AssignMarkersValidation
			with AssignMarkersPermissions
			with AssignMarkersDescription
			with AutowiringUserLookupComponent
			with AutowiringCM2MarkingWorkflowServiceComponent
}

object AssignMarkersBySpreadsheetCommand {

	def apply(assignment: Assignment) =
		new AssignMarkersCommandInternal(assignment)
			with ComposableCommand[Assignment]
			with AssignMarkersBySpreadsheetState
			with AssignMarkersBySpreadsheetBindListener
			with AssignMarkersValidation
			with AssignMarkersPermissions
			with AssignMarkersDescription
			with AutowiringUserLookupComponent
			with AutowiringCM2MarkingWorkflowServiceComponent
			with AutowiringMarkerAllocationExtractorComponent

	val AcceptedFileExtensions = Seq(".xlsx")
}

abstract class AssignMarkersCommandInternal(val assignment: Assignment) extends CommandInternal[Assignment] {

	this: UserLookupComponent with CM2MarkingWorkflowServiceComponent with AssignMarkersState =>

	def applyInternal(): Assignment = {
		assignment.cm2MarkingWorkflow.allStages.foreach(stage => {
			val allocations = allocationMap.getOrElse(stage, Map())
			cm2MarkingWorkflowService.allocateMarkersForStage(assignment, stage, allocations)
		})

		assignment
	}
}

trait AssignMarkersBySpreadsheetBindListener extends BindListener {

	this: UserLookupComponent with CM2MarkingWorkflowServiceComponent with MarkerAllocationExtractorComponent
		with AssignMarkersBySpreadsheetState =>

	override def onBind(result: BindingResult): Unit = transactional() {

		val fileNames = file.fileNames.map(_.toLowerCase)
		val invalidFiles = fileNames.filter(s => !AssignMarkersBySpreadsheetCommand.AcceptedFileExtensions.exists(s.endsWith))

		if (invalidFiles.nonEmpty) {
			if (invalidFiles.size == 1) result.rejectValue("file", "file.wrongtype.one", Array(invalidFiles.mkString("")), "")
			else result.rejectValue("", "file.wrongtype", Array(invalidFiles.mkString(", ")), "")
		}

		if (!result.hasErrors) {
			transactional() {
				file.onBind(result)
				if (!file.attached.isEmpty) {

					val sheetData = markerAllocationExtractor
						.extractMarkersFromSpreadsheet(file.attached.asScala.head.dataStream, assignment.cm2MarkingWorkflow)

					def rowsToAllocations(rows: Seq[ParsedRow]): Allocations = rows
						.filter(_.errors.isEmpty)
						.groupBy(a => a.marker)
						.collect{ case (Some(marker), r) => marker -> r.flatMap(_.student).toSet }

					rowsWithErrors = sheetData.flatMap{ case(_, rows) => rows}.filter(_.errors.nonEmpty).toSeq

					_allocationMap = sheetData.flatMap{ case(key, rows) =>
						if(assignment.cm2MarkingWorkflow.workflowType.rolesShareAllocations) {
							val stages = assignment.cm2MarkingWorkflow.allStages.filter(_.roleName == key)
							stages.map(s => s -> rowsToAllocations(rows))
						} else {
							Seq(MarkingWorkflowStage.fromCode(key) -> rowsToAllocations(rows))
						}
					}

				}
			}
		}
	}
}

trait AssignMarkersPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AssignMarkersState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Assignment.Update, assignment.module)
	}
}

trait AssignMarkersValidation extends SelfValidating {
	self: AssignMarkersState =>
	def validate(errors: Errors) {}
}

trait AssignMarkersDescription extends Describable[Assignment] {
	self: AssignMarkersState =>

	private def printAllocation(allocation: Allocations): String = allocation.map{ case(marker, students) =>
			s"${marker.getUserId} -> ${students.map(_.getUserId).mkString(",")}"
	}.mkString("\n")

	def describe(d: Description) {
		d.assignment(assignment)
		 .properties("allocations" -> allocationMap.map{
				case(stage, allocation) => s"${stage.roleName}:\n${printAllocation(allocation)}"
			}.mkString("\n"))
	}
}

trait AssignMarkersState {
	def assignment: Assignment
	def allocationMap: Map[MarkingWorkflowStage, Allocations]
}

trait AssignMarkersBySpreadsheetState extends AssignMarkersState {
	def allocationMap: Map[MarkingWorkflowStage, Allocations] = _allocationMap
	protected var _allocationMap: Map[MarkingWorkflowStage, Allocations] = Map()
	var rowsWithErrors: Seq[ParsedRow] = Nil
	var file: UploadedFile = new UploadedFile
}

trait AssignMarkersDragAndDropState extends AssignMarkersState {

	this: UserLookupComponent =>

	// the nasty java mutable bindable map transformed into lovely immutable scala maps with the users resolved
	def allocationMap: Map[MarkingWorkflowStage, Allocations] = {
		allocations.asScala.map{ case (stage, jmap) =>
			stage -> jmap.asScala.map{ case (marker, students) =>
				userLookup.getUserByUserId(marker) -> students.asScala.map(userLookup.getUserByUserId).toSet
			}.toMap
		}.toMap
	}

	// will this bind? who knows! - spring is a capricious monster
	var allocations: JMap[MarkingWorkflowStage, JMap[String, JList[String]]] = LazyMaps.create{ _: MarkingWorkflowStage =>
		LazyMaps.create{ _: String => JArrayList(): JList[String] }.asJava
	}.asJava
}