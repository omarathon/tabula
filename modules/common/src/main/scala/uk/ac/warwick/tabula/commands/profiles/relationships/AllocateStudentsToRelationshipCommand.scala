package uk.ac.warwick.tabula.commands.profiles.relationships

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.profiles.relationships.AllocateStudentsToRelationshipCommand.Result
import uk.ac.warwick.tabula.commands.profiles.relationships.ExtractRelationshipsFromFileCommand.AllocationTypes
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.profiles.{BulkNewAgentRelationshipNotification, BulkOldAgentRelationshipNotification, BulkStudentRelationshipNotification}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringRelationshipServiceComponent, ProfileServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}

import scala.collection.JavaConverters._

object AllocateStudentsToRelationshipCommand {

	case class Result(expiredRelationships: Seq[StudentRelationship], addedRelationships: Seq[StudentRelationship])

	def apply(department: Department, relationshipType: StudentRelationshipType, user: CurrentUser) =
		new AllocateStudentsToRelationshipCommandInternal(department, relationshipType, user)
			with AutowiringRelationshipServiceComponent
			with AutowiringProfileServiceComponent
			with ComposableCommand[Result]
			with AllocateStudentsToRelationshipValidation
			with AllocateStudentsToRelationshipDescription
			with AllocateStudentsToRelationshipPermissions
			with AllocateStudentsToRelationshipCommandState
			with AllocateStudentsToRelationshipCommandRequest
			with AllocateStudentsToRelationshipNotifications
}


class AllocateStudentsToRelationshipCommandInternal(val department: Department, val relationshipType: StudentRelationshipType, val user: CurrentUser)
	extends CommandInternal[AllocateStudentsToRelationshipCommand.Result] {

	self: AllocateStudentsToRelationshipCommandRequest with AllocateStudentsToRelationshipCommandState with RelationshipServiceComponent =>

	override def applyInternal(): Result = {
		val relationshipsToExpire: Seq[StudentRelationship] = (allocationType match {
			case AllocationTypes.Replace =>
				relationshipsNotInAdditions.groupBy(_._2).mapValues(_.map(_._1)).flatMap { case(entity, studentsToRemove) =>
					relationshipService.listCurrentRelationshipsWithAgent(relationshipType, entity.entityId)
						.filter(_.studentMember.exists(s => studentsToRemove.map(_.universityId).contains(s.universityId)))
				}.toSeq
			case AllocationTypes.Add =>
				removals.asScala.flatMap { case(entityId, removeIDs) =>
					relationshipService.listCurrentRelationshipsWithAgent(relationshipType, entityId)
						.filter(_.studentMember.exists(s => removeIDs.contains(s.universityId)))
				}.toSeq
			case _ =>
				Seq()
		}).filter (_.studentCourseDetails.mostSignificant)

		relationshipService.endStudentRelationships(relationshipsToExpire)

		/**TODO- If a student is on 2 courses (mainly course transfer cases)-> when we create a new relationship for most significant course Ist time
			* If there is another course that is not most significant one, set end date for that particular relationship type with the same agent (if it is blank)
			* Also would need to move comments etc from old relationship to this new one. Probably part of JIRA TAB-4555 **/
		val newRelationships: Seq[StudentRelationship] = additions.asScala.flatMap{case(entityId, addIDs) =>
			val alreadyExist = dbAllocated.find(_.entityId == entityId).map(e => e.students.map(_.universityId)).getOrElse(Seq())
			relationshipService.applyStudentRelationships(relationshipType, entityId, addIDs.asScala.toSeq.filterNot(alreadyExist.contains))
		}.toSeq

		AllocateStudentsToRelationshipCommand.Result(relationshipsToExpire, newRelationships)
	}

}

trait AllocateStudentsToRelationshipValidation extends SelfValidating {

	self: AllocateStudentsToRelationshipCommandState with AllocateStudentsToRelationshipCommandRequest =>

	override def validate(errors: Errors) {
		// Should never happen in practice, but protects against direct POSTs

		val allChangedStudentIDs = additions.asScala.flatMap(_._2.asScala) ++ removals.asScala.flatMap(_._2.asScala)
		val notFound = allChangedStudentIDs.filterNot(allStudents.map(_.universityId).contains)
		if (notFound.nonEmpty) {
			errors.rejectValue("additions", "profiles.relationship.allocate.students.notFound", allChangedStudentIDs.toArray, "")
		}

		if (!allocationType.hasText || !Seq(AllocationTypes.Replace, AllocationTypes.Add).contains(allocationType)) {
			errors.rejectValue("allocationType", "profiles.relationship.allocate.allocationType.empty")
		}
	}

}

trait AllocateStudentsToRelationshipPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AllocateStudentsToRelationshipCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		// throw this request out if this relationship can't be edited in Tabula for this department
		if (relationshipType.readOnly(department)) {
			logger.info("Denying access to FetchDepartmentRelationshipInformationCommand since relationshipType %s is read-only".format(relationshipType))
			throw new ItemNotFoundException()
		}
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Manage(mandatory(relationshipType)), mandatory(department))
	}

}

trait AllocateStudentsToRelationshipDescription extends Describable[AllocateStudentsToRelationshipCommand.Result] {

	self: AllocateStudentsToRelationshipCommandState with AllocateStudentsToRelationshipCommandRequest =>

	override lazy val eventName = "AllocateStudentsToRelationship"

	override def describe(d: Description) {
		d.department(department)
		d.studentRelationshipType(relationshipType)
		d.property("allocationType", allocationType)
	}

	override def describeResult(d: Description, result: AllocateStudentsToRelationshipCommand.Result): Unit = {
		d.property("expiredRelationships", result.expiredRelationships.map(_.id))
		d.property("addedRelationships", result.addedRelationships.map(_.id))
	}
}

trait AllocateStudentsToRelationshipCommandState {
	def department: Department
	def relationshipType: StudentRelationshipType
	def user: CurrentUser
}

trait AllocateStudentsToRelationshipCommandRequest {

	self: AllocateStudentsToRelationshipCommandState with RelationshipServiceComponent =>

	var additionalEntities: JList[String] = JArrayList()

	lazy val dbUnallocated: Seq[StudentAssociationData] = relationshipService.getStudentAssociationDataWithoutRelationship(department, relationshipType, Seq())
	lazy val dbAllocated: Seq[StudentAssociationEntityData] = relationshipService.getStudentAssociationEntityData(department, relationshipType, additionalEntities.asScala)
	lazy val allStudents: Seq[StudentAssociationData] = dbUnallocated ++ dbAllocated.flatMap(_.students).distinct

	var additions: JMap[String, JList[String]] =
		LazyMaps.create{entityId: String => JArrayList(): JList[String] }.asJava

	lazy val renderAdditions: Map[StudentAssociationData, Seq[String]] = additions.asScala.toSeq.flatMap{case(entityId, addIDs) =>
		dbAllocated.find(_.entityId == entityId).map { entity =>
			addIDs.asScala
				.filterNot(s => entity.students.map(_.universityId).contains(s)) // Don't re-add existing relationships
				.flatMap(id => allStudents.find(_.universityId == id).map(student => (student, entity.displayName)))
		}.getOrElse(Seq())
	}.groupBy(_._1).mapValues(pairs => pairs.map(_._2))

	var removals: JMap[String, JList[String]] =
		LazyMaps.create{entityId: String => JArrayList(): JList[String] }.asJava

	lazy val relationshipsNotInAdditions: Seq[(StudentAssociationData, StudentAssociationEntityData)] = {
		val allStudentAdditionIds = additions.asScala.flatMap(_._2.asScala).toSeq.distinct
		def inAdditions(entityId: String, studentId: String) = additions.asScala.get(entityId).exists(_.asScala.contains(studentId))

		dbAllocated.flatMap{entity =>
			entity.students.flatMap(student =>
				if (inAdditions(entity.entityId, student.universityId)) // Don't remove and add the same relationship
					None
				else if (allStudentAdditionIds.contains(student.universityId)) // Remove the existing relationship
					Some((student, entity))
				else
					None
			)
		}
	}

	lazy val renderRemovals: Map[StudentAssociationData, Seq[String]] = allocationType match {
		case AllocationTypes.Replace =>
			relationshipsNotInAdditions.map{case(student, entity) => (student, entity.displayName)}.groupBy(_._1).mapValues(pairs => pairs.map(_._2))
		case AllocationTypes.Add =>
			removals.asScala.toSeq.flatMap{case(entityId, removeIDs) =>
				dbAllocated.find(_.entityId == entityId).map { entity =>
					removeIDs.asScala.flatMap(id => entity.students.find(_.universityId == id).map(student => (student, entity.displayName)))
				}.getOrElse(Seq())
			}.groupBy(_._1).mapValues(pairs => pairs.map(_._2))
		case _ => Map()
	}

	var allocationType: String = ""

	var notifyStudent: Boolean = false
	var notifyOldAgents: Boolean = false
	var notifyNewAgent: Boolean = false

}

trait AllocateStudentsToRelationshipNotifications extends Notifies[AllocateStudentsToRelationshipCommand.Result, Seq[StudentRelationship]] {

	self: ProfileServiceComponent with AllocateStudentsToRelationshipCommandState with AllocateStudentsToRelationshipCommandRequest =>

	def emit(commandResult: AllocateStudentsToRelationshipCommand.Result): Seq[Notification[StudentRelationship, Unit]] = {

		val studentNotifications = {
			if (notifyStudent) {
				val removalsByStudent = commandResult.expiredRelationships.groupBy(_.studentMember)
				val additionsByStudent = commandResult.addedRelationships.groupBy(_.studentMember)
				val allStudents: Set[StudentMember] = (removalsByStudent.keySet ++ additionsByStudent.keySet).flatten
				allStudents.map { student =>
					val notification = Notification.init(
						new BulkStudentRelationshipNotification,
						user.apparentUser,
						additionsByStudent.getOrElse(Some(student), Seq()) ++ removalsByStudent.getOrElse(Some(student), Seq())
					)
					notification.profileService = profileService // the auto-wired version is no good for testing
					notification.oldAgentIds.value = removalsByStudent.getOrElse(Some(student), Seq()).map(_.agent)
					notification
				}
			} else {
				Nil
			}
		}.toSeq

		val newAgentNotifications = if (notifyNewAgent) {
			commandResult.addedRelationships.groupBy(_.agent).flatMap { case (agentId, relationships) =>
				relationships.head.agentMember.map { agent =>
					val notification = Notification.init(new BulkNewAgentRelationshipNotification, user.apparentUser, relationships)
					notification.profileService = profileService // the auto-wired version is no good for testing
					notification
				}
			}
		} else {
			Nil
		}.toSeq

		val oldAgentNotifications = if (notifyOldAgents) {
			commandResult.expiredRelationships.groupBy(_.agent).flatMap { case (agentId, relationships) =>
				relationships.head.agentMember.map { agent =>
					val notification = Notification.init(new BulkOldAgentRelationshipNotification, user.apparentUser, relationships)
					notification.profileService = profileService // the auto-wired version is no good for testing
					notification
				}
			}
		} else {
			Nil
		}.toSeq

		studentNotifications.toSeq ++ oldAgentNotifications ++ newAgentNotifications
	}

}
