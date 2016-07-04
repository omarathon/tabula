package uk.ac.warwick.tabula.commands.groups.admin

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.admin.ImportSmallGroupSetsFromSpreadsheetCommand._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model.{Department, MapLocation, NamedLocation}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.groups.docconversion._
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object ImportSmallGroupSetsFromSpreadsheetCommand {
	val RequiredPermission = Permissions.SmallGroups.ImportFromExternalSystem
	type CommandType = Appliable[Seq[SmallGroupSet]] with SelfValidating with BindListener

	type SetCommand = Appliable[SmallGroupSet] with SelfValidating
	type GroupCommand = Appliable[SmallGroup] with SelfValidating
	type EventCommand = Appliable[SmallGroupEvent] with SelfValidating

	def apply(department: Department, academicYear: AcademicYear): CommandType =
		new ImportSmallGroupSetsFromSpreadsheetCommandInternal(department, academicYear)
			with ComposableCommand[Seq[SmallGroupSet]]
			with ImportSmallGroupSetsFromSpreadsheetPermissions
			with ImportSmallGroupSetsFromSpreadsheetDescription
			with ImportSmallGroupSetsFromSpreadsheetValidation
			with ImportSmallGroupSetsFromSpreadsheetBinding
			with AutowiringSmallGroupSetSpreadsheetHandlerComponent
			with AutowiringSmallGroupServiceComponent
}

abstract class ImportSmallGroupSetsFromSpreadsheetCommandInternal(val department: Department, val academicYear: AcademicYear) extends CommandInternal[Seq[SmallGroupSet]]
	with ImportSmallGroupSetsFromSpreadsheetRequest {

	override def applyInternal(): Seq[SmallGroupSet] = commands.map { sHolder =>
		val set = sHolder.command.apply()

		sHolder.groupCommands.foreach { gHolder =>
			gHolder.command match {
				case c: CreateSmallGroupCommandInternal => c.set = set
				case _ =>
			}

			val group = gHolder.command.apply()

			gHolder.eventCommands.foreach { eHolder =>
				eHolder.command match {
					case c: CreateSmallGroupEventCommandInternal =>
						c.set = set
						c.group = group
					case _ =>
				}

				eHolder.command.apply()
			}
		}

		set
	}

}

trait ImportSmallGroupSetsFromSpreadsheetValidation extends SelfValidating {
	self: ImportSmallGroupSetsFromSpreadsheetRequest =>

	override def validate(errors: Errors): Unit = {
		commands.zipWithIndex.foreach { case (set, i) =>
			errors.pushNestedPath(s"commands[$i]")

			set.command.validate(errors)
			set.groupCommands.zipWithIndex.foreach { case (group, j) =>
				errors.pushNestedPath(s"groupCommands[$j]")

				group.command.validate(errors)
				group.eventCommands.zipWithIndex.foreach { case (event, k) =>
					errors.pushNestedPath(s"eventCommands[$k]")
					event.command.validate(errors)
					errors.popNestedPath()
				}

				errors.popNestedPath()
			}

			errors.popNestedPath()
		}
	}

}

trait ImportSmallGroupSetsFromSpreadsheetBinding extends BindListener {
	self: ImportSmallGroupSetsFromSpreadsheetRequest
		with SmallGroupSetSpreadsheetHandlerComponent
		with SmallGroupServiceComponent =>

	override def onBind(result: BindingResult): Unit = {
		if (file.isMissing) {
			result.rejectValue("file", "NotEmpty")
		} else {
			val fileNames = file.fileNames map (_.toLowerCase)
			val invalidFiles = fileNames.filter(s => !SmallGroupSetSpreadsheetHandler.AcceptedFileExtensions.exists(s.endsWith))

			if (invalidFiles.nonEmpty) {
				if (invalidFiles.size == 1) result.rejectValue("file", "file.wrongtype.one", Array(invalidFiles.mkString("")), "")
				else result.rejectValue("file", "file.wrongtype", Array(invalidFiles.mkString(", ")), "")
			}
		}

		if (!result.hasErrors) {
			transactional() {
				file.onBind(result)
				commands = file.attached.asScala.filter(_.hasData).flatMap { attachment =>
					val extractedSets = smallGroupSetSpreadsheetHandler.readXSSFExcelFile(department, academicYear, attachment.dataStream, result)

					// Convert to commands
					extractedSets.map { extracted =>
						val existing =
							smallGroupService.getSmallGroupSets(extracted.module, academicYear)
								.find { s => s.name.equalsIgnoreCase(extracted.name) }

						val (setCommand, setCommandType) = existing match {
							case Some(set) => (ModifySmallGroupSetCommand.edit(set.module, set), "Edit")
							case _ => (ModifySmallGroupSetCommand.create(extracted.module), "Create")
						}

						setCommand.name = extracted.name
						setCommand.format = extracted.format
						setCommand.allocationMethod = extracted.allocationMethod
						setCommand.allowSelfGroupSwitching = extracted.studentsCanSwitchGroup
						setCommand.studentsCanSeeTutorName = extracted.studentsSeeTutor
						setCommand.studentsCanSeeOtherMembers = extracted.studentsSeeStudents
						setCommand.collectAttendance = extracted.collectAttendance
						setCommand.linkedDepartmentSmallGroupSet = extracted.linkedSmallGroupSet.orNull

						def matchesGroup(extractedGroup: ExtractedSmallGroup)(smallGroup: SmallGroup) =
							smallGroup.name.equalsIgnoreCase(extractedGroup.name)

						def matchesEvent(extractedEvent: ExtractedSmallGroupEvent)(smallGroupEvent: SmallGroupEvent) =
							(extractedEvent.title.nonEmpty && extractedEvent.title.contains(smallGroupEvent.title)) ||
							(extractedEvent.weekRanges == smallGroupEvent.weekRanges && extractedEvent.dayOfWeek == smallGroupEvent.day && extractedEvent.startTime == smallGroupEvent.startTime)

						val groupCommands = extracted.groups.map { extractedGroup =>
							val existingGroup = existing.toSeq.flatMap(_.groups.asScala).find(matchesGroup(extractedGroup))

							val (groupCommand, groupCommandType) = existingGroup match {
								case Some(group) => (ModifySmallGroupCommand.edit(group.groupSet.module, group.groupSet, group), "Edit")
								case _ => (ModifySmallGroupCommand.create(extracted.module, new SmallGroupSet), "Create")
							}

							groupCommand.name = extractedGroup.name
							groupCommand.maxGroupSize = extractedGroup.limit.getOrElse(SmallGroup.DefaultGroupSize)

							val eventCommands = extractedGroup.events.map { extractedEvent =>
								val existingEvent = existingGroup.toSeq.flatMap(_.events).find(matchesEvent(extractedEvent))

								val (eventCommand, eventCommandType) = existingEvent match {
									case Some(event) => (ModifySmallGroupEventCommand.edit(event.group.groupSet.module, event.group.groupSet, event.group, event), "Edit")
									case _ => (ModifySmallGroupEventCommand.create(extracted.module, new SmallGroupSet, new SmallGroup), "Create")
								}

								eventCommand.title = extractedEvent.title.orNull
								eventCommand.tutors = extractedEvent.tutors.map(_.getUserId).asJava
								eventCommand.weekRanges = extractedEvent.weekRanges
								eventCommand.day = extractedEvent.dayOfWeek
								eventCommand.startTime = extractedEvent.startTime
								eventCommand.endTime = extractedEvent.endTime

								extractedEvent.location.foreach {
									case NamedLocation(name) =>
										eventCommand.location = name
										eventCommand.locationId = null
									case MapLocation(name, lid) =>
										eventCommand.location = name
										eventCommand.locationId = lid
								}

								new ModifySmallGroupEventCommandHolder(eventCommand, eventCommandType)
							}

							val deleteEventCommands =
								existingGroup.toSeq.flatMap(_.events.sorted)
									.filterNot { e => extractedGroup.events.exists(matchesEvent(_)(e)) }
									.map { e => new ModifySmallGroupEventCommandHolder(DeleteSmallGroupEventCommand(e.group, e), "Delete") }

							new ModifySmallGroupCommandHolder(groupCommand, groupCommandType, eventCommands ++ deleteEventCommands)
						}

						val deleteGroupCommands =
							existing.toSeq.flatMap(_.groups.asScala.sorted)
								.filterNot { g => extracted.groups.exists(matchesGroup(_)(g)) }
								.map { g => new ModifySmallGroupCommandHolder(DeleteSmallGroupCommand(g.groupSet, g), "Delete", Nil) }

						new ModifySmallGroupSetCommandHolder(setCommand, setCommandType, groupCommands ++ deleteGroupCommands)
					}
				}
			}
		}
	}
}

class ModifySmallGroupSetCommandHolder(var command: SetCommand, val commandType: String, var groupCommands: Seq[ModifySmallGroupCommandHolder])
class ModifySmallGroupCommandHolder(var command: GroupCommand, val commandType: String, var eventCommands: Seq[ModifySmallGroupEventCommandHolder])
class ModifySmallGroupEventCommandHolder(var command: EventCommand, val commandType: String)

trait ImportSmallGroupSetsFromSpreadsheetRequest extends ImportSmallGroupSetsFromSpreadsheetState {
	var file: UploadedFile = new UploadedFile
	var confirm: Boolean = _

	// Bound by BindListener (ImportSmallGroupSetsFromSpreadsheetBinding)
	var commands: Seq[ModifySmallGroupSetCommandHolder] = Nil
}

trait ImportSmallGroupSetsFromSpreadsheetState {
	def department: Department
	def academicYear: AcademicYear
}

trait ImportSmallGroupSetsFromSpreadsheetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ImportSmallGroupSetsFromSpreadsheetState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = p.PermissionCheck(RequiredPermission, mandatory(department))
}

trait ImportSmallGroupSetsFromSpreadsheetDescription extends Describable[Seq[SmallGroupSet]] {
	self: ImportSmallGroupSetsFromSpreadsheetState =>

	override def describe(d: Description) =
		d.department(department)
}