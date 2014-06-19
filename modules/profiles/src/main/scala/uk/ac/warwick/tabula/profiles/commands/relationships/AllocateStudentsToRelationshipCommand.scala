package uk.ac.warwick.tabula.profiles.commands.relationships

import scala.collection.JavaConverters._
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.commands.{GroupsObjectsWithFileUpload, MemberCollectionHelper, SelfValidating, Command, Description}
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
import uk.ac.warwick.tabula.data.model.StudentMember

class AllocateStudentsToRelationshipCommand(val department: Department, val relationshipType: StudentRelationshipType, val viewer: CurrentUser)
	extends Command[Seq[StudentRelationshipChange]]
		with GroupsObjectsWithFileUpload[Member, Member]
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
	implicit val defaultOrderingForMember = Ordering.by { m: Member => (Option(m.lastName), Option(m.firstName), Option(m.universityId)) }

	val apparentUser = viewer.apparentUser

	var service = Wire[RelationshipService]
	var profileService = Wire[ProfileService]
	var securityService = Wire[SecurityService]

	var relationshipExtractor = Wire[RawStudentRelationshipExtractor]

	var additionalAgents: JList[String] = JArrayList()

	var previouslyAllocatedMapping: JMap[Member, JList[Member]] = _ // populated by hidden field in form
	
	override def onBind(result: BindingResult) {
		super.onBind(result)

		removeBlankAgents()

		// 'mapping' contains a map from agent to students, populated in the form.
		// When new agents are added through the "Add <e.g. personal tutors>" modal, add them to mapping
		additionalAgents.asScala
			.flatMap { profileService.getAllMembersWithUserId(_) }
			.foreach { member =>
				if (!mapping.containsKey(member)) mapping.put(member, JArrayList())
			}

		def removeBlankAgents() {
			// Find all empty textboxes for agents and remove them - otherwise we end up with a never ending list of empties
			val indexesToRemove = additionalAgents.asScala.zipWithIndex.flatMap { case (agent, index) =>
				if (!agent.hasText) Some(index)
				else None
			}

			// We reverse because removing from the back is better
			indexesToRemove.reverse.foreach {
				additionalAgents.remove(_)
			}
		}
	}

	/**
	 * populate - only called on initial form view
	 *
	 * Populate 'mapping' with existing relationships between agents and students, from the database.
	 * Also set up unallocated as a list of all students in the department without this type of agent.
	 *
	 */
	override def populate() {
		service
			.listStudentRelationshipsByDepartment(relationshipType, department) // get all relationships by dept
			.groupBy(_.agent) // group into map by agent university id
			.foreach { case (agent, students) =>
				if (agent.forall(_.isDigit)) {
					profileService.getMemberByUniversityId(agent) match {
						case Some(member) =>
							mapping.put(member, JArrayList(students.flatMap(_.studentMember).toList))
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
	def mappingById = (mapping.asScala.map {
		case (member, users) => (member.universityId, users)
	}).toMap

	// For use by Freemarker to get a simple map of university IDs to Member objects - permissions aware!
	lazy val membersById = loadMembersById

	// sets up 'members' - a map from uni ID to member object for both agents and students
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
		val agentsBefore = previouslyAllocatedMapping.keySet.asScala
		val agentsAfter = mapping.keySet.asScala

		val newAgents = agentsAfter.filterNot(agentsBefore.contains(_))
		val droppedAgents = agentsBefore.filterNot(agentsAfter.contains(_))
		val changedAgents = agentsAfter.intersect(agentsBefore).filterNot(agent => previouslyAllocatedMapping.get(agent).equals(mapping.get(agent)))

		// get the commands needed to remove the relationships of this type for dropped and changed agents:
		val removeCommands = for (
			agent <- droppedAgents ++ changedAgents;
			relationship <- service.listStudentRelationshipsWithMember(relationshipType, agent)) yield {
			new EndStudentRelationshipCommand(relationship, viewer)
		}

		val addCommands = for (agent <- newAgents ++ changedAgents) {
			val studentSet = mapping.asScala.get(agent).flatMap(_.asScala)

			val studentMembersForAgent = for (student <- studentSet) yield {
				val studentMember: Option[StudentMember] = student match {
					case stuMem: StudentMember => Some(stuMem)
					case _ => None
				}
			}

			val addCommandsForAgent = (studentMembersForAgent.map {
				stu => stu.mostSignificantCourseDetails.map {
					new EditStudentRelationshipCommand(_, relationshipType, Some(agent), viewer, false)
				}
			}).flatten
			addCommandsForAgent

		}

		addCommands.flatten

		val result = (addCommands ++ removeCommands).map { cmd =>
			/*
			 * Defensively code against these defaults changing in future. We do NOT want the
			 * sub-command to send notifications - we'll do that ourselves
			 */
			cmd.notifyStudent = false
			cmd.notifyOldAgent = false
			cmd.notifyNewAgent = false

			cmd.apply().map { modifiedRelationship => StudentRelationshipChange(cmd.oldAgent, modifiedRelationship.asInstanceOf[StudentRelationship]) }
		}
		result.flatten
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
