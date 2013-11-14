package uk.ac.warwick.tabula.profiles.commands.relationships

import scala.collection.JavaConverters._
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.commands.{MemberCollectionHelper, SelfValidating, Command, Description, GroupsObjects}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.profiles.services.docconversion.RawStudentRelationshipExtractor
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

class AllocateStudentsToRelationshipCommand(val department: Department, val relationshipType: StudentRelationshipType, val viewer: CurrentUser)
	extends Command[Seq[StudentRelationshipChange]]
		with GroupsObjects[Member, Member]
		with SelfValidating
		with BindListener
		with RelationshipChangingCommand
		with MemberCollectionHelper
		with NotifiesAffectedStudents {

	PermissionCheck(Permissions.Profiles.StudentRelationship.Update(mandatory(relationshipType)), mandatory(department))

	// throw this request out if this relationship can't be edited in Tabula for this department
	if (relationshipType.readOnly(department)) {
		logger.info(
			"Denying access to AllocateStudentsToRelationshipCommand since relationshipType %s is read-only"
			.format(relationshipType)
		)
		throw new ItemNotFoundException()
	}

	// Sort members by last name, first name
	implicit val defaultOrderingForMember = Ordering.by[Member, String] ( user => user.lastName + ", " + user.firstName )

	val apparentUser = viewer.apparentUser

	var service = Wire[RelationshipService]
	var profileService = Wire[ProfileService]
	var securityService = Wire[SecurityService]

	var relationshipExtractor = Wire[RawStudentRelationshipExtractor]

	var additionalAgents: JList[String] = JArrayList()

	override def onBind(result: BindingResult) {
		super.onBind(result)

		// Find all empty textboxes for agents and remove them - otherwise we end up with a never ending list of empties
		val indexesToRemove = additionalAgents.asScala.zipWithIndex.flatMap { case (agent, index) =>
			if (!agent.hasText) Some(index)
			else None
		}

		// We reverse because removing from the back is better
		indexesToRemove.reverse.foreach { additionalAgents.remove(_) }

		additionalAgents.asScala
			.flatMap { profileService.getMemberByUserId(_) }
			.foreach { member =>
				if (!mapping.containsKey(member)) mapping.put(member, JArrayList())
			}
	}

	// Only called on initial form view
	override def populate() {
		def studentRelationshipToMember(rel: StudentRelationship) = profileService.getStudentBySprCode(rel.targetSprCode)

		// get all relationships by dept
		service
			.listStudentRelationshipsByDepartment(relationshipType, department)
			.groupBy(_.agent) // group into map by agent university id
			.foreach { case (agent, students) =>
				if (agent.forall(_.isDigit)) {
					profileService.getMemberByUniversityId(agent) match {
						case Some(member) =>
							mapping.put(member, JArrayList(students.flatMap(studentRelationshipToMember).toList))
						case _ => // do nothing
					}
				}
			}

		unallocated.clear()
		unallocated.addAll(
			service
				.listStudentsWithoutRelationship(relationshipType, department)
				.asJavaCollection
		)
	}

	// Purely for use by Freemarker as it can't access map values unless the key is a simple value.
	// Do not modify the returned value!
	override def mappingById = (mapping.asScala.map {
		case (member, users) => (member.universityId, users)
	}).toMap

	// For use by Freemarker to get a simple map of university IDs to Member objects - permissions aware!
	lazy val membersById = loadMembersById

	def loadMembersById = {
		val members =
			(unallocated.asScala ++ (for ((agent, students) <- mapping.asScala) yield agent +: students.asScala).flatten)
			.filter(member => securityService.can(viewer, Permissions.Profiles.Read.Core, member))
			.map(member => (member.universityId, member)).toMap
		members
	}

	def allMembersRoutes = {
		allMembersRoutesSorted(membersById.values)
	}

	def allMembersYears: Seq[JInteger] = {
		allMembersYears(membersById.values)
	}

	// Sort all the lists of users by surname, firstname.
	override def sort() {
		// Because sortBy is not an in-place sort, we have to replace the lists entirely.
		// Alternative is Collections.sort or math.Sorting but these would be more code.
		for ((agent, users) <- mapping.asScala) {
			mapping.put(agent, JArrayList(users.asScala.toList.sorted))
		}

		unallocated = JArrayList(unallocated.asScala.toList.sorted)
	}

	final def applyInternal() = transactional() {
		val addCommands = (for ((agent, students) <- mapping.asScala; student <- students.asScala) yield {
			student.mostSignificantCourseDetails.map { studentCourseDetails =>
				val cmd = new EditStudentRelationshipCommand(
					studentCourseDetails,
					relationshipType,
					service.findCurrentRelationships(relationshipType, studentCourseDetails.sprCode).headOption.flatMap { _.agentMember }, 
					viewer, 
					false
				)
				cmd.agent = agent
				cmd
			}
		}).toSeq.flatten

		val removeCommands = unallocated.asScala.flatMap { student =>
			student.mostSignificantCourseDetails.map { studentCourseDetails =>
				val rels = service.findCurrentRelationships(relationshipType, studentCourseDetails.sprCode)
				val agents = rels.flatMap { _.agentMember }

				agents.map { agent =>
					val cmd = new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, Some(agent), viewer, true)
					cmd.agent = agent
					cmd
				}
			}
		}.toSeq.flatten

		(addCommands ++ removeCommands).map { cmd =>
			/*
			 * Defensively code against these defaults changing in future. We do NOT want the
			 * sub-command to send notifications - we'll do that ourselves
			 */
			cmd.notifyStudent = false
			cmd.notifyOldAgent = false
			cmd.notifyNewAgent = false

			cmd.apply().map { modifiedRelationship => StudentRelationshipChange(cmd.currentAgent, modifiedRelationship) }
		}.flatten
	}
	
	def validateUploadedFile(result: BindingResult) {
		val fileNames = file.fileNames map (_.toLowerCase)
		val invalidFiles = fileNames.filter(s => !RawStudentRelationshipExtractor.AcceptedFileExtensions.exists(s.endsWith))

		if (invalidFiles.size > 0) {
			if (invalidFiles.size == 1) result.rejectValue("file", "file.wrongtype.one", Array(invalidFiles.mkString("")), "")
			else result.rejectValue("", "file.wrongtype", Array(invalidFiles.mkString(", ")), "")
		}
	}

	def extractDataFromFile(file: FileAttachment, result: BindingResult) = {
		val allocations = relationshipExtractor.readXSSFExcelFile(file.dataStream, relationshipType)
		
		// Put any errors into the BindingResult
		allocations.foreach { case (row, _, errors) =>
			errors.foreach { case (field, code) =>
				result.rejectValue("", code, Array(field, row), "")
			}
		}
		
		val rawRelationships = allocations.flatMap { case (_, rel, _) => rel }

		unallocated.clear()
		unallocated.addAll(
			rawRelationships.filter { case (_, staff) => staff.isEmpty }
			   .map { case (student, _) => student }
			   .asJavaCollection
		)

		rawRelationships
			.filter { case (_, staff) => staff.isDefined }
		   	.map { case (student, staff) => (student, staff.get) }
		   	.groupBy { case (_, staff) => staff }
		   	.mapValues { values =>
	   			values.map { case (student, _) => student }.asJava
		   	}
	}

	def validate(errors: Errors) {
		// Nothing to do
	}

	def describe(d: Description) = d.department(department)

}

case class StudentRelationshipChange(
	oldAgent: Option[Member],
	modifiedRelationship: StudentRelationship
)
