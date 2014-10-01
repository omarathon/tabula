package uk.ac.warwick.tabula.profiles.commands.relationships

import scala.collection.JavaConverters._
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.commands.{GroupsObjectsWithFileUpload, MemberCollectionHelper, SelfValidating, Command, Description}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService, SecurityService}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.profiles.services.docconversion.RawStudentRelationshipExtractor
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.data.model.StudentMember

case class TutorInfoForStudent(tutorsBefore: Set[Member], tutorsAfter: Set[Member]) {
	def oldTutors = tutorsBefore -- tutorsAfter
	def newTutors = tutorsAfter -- tutorsBefore
}

class AllocateStudentsToRelationshipCommand(val department: Department, val relationshipType: StudentRelationshipType, val viewer: CurrentUser)
	extends Command[Seq[StudentRelationshipChange]]
		with GroupsObjectsWithFileUpload[Member, Member]
		with SelfValidating
		with BindListener
		with RelationshipChangingCommand
		with MemberCollectionHelper
		with NotifiesAffectedStudents
		with FetchesRelationshipMappings {

	PermissionCheck(Permissions.Profiles.StudentRelationship.Update(mandatory(relationshipType)), mandatory(department))

	// throw this request out if this relationship can't be edited in Tabula for this department
	if (relationshipType.readOnly(department)) {
		logger.info(
			"Denying access to AllocateStudentsToRelationshipCommand since relationshipType %s is read-only"
			.format(relationshipType)
		)
		throw new ItemNotFoundException()
	}

	var securityService = Wire[SecurityService]
	var relationshipService = Wire[RelationshipService]
	var profileService = Wire[ProfileService]
	var relationshipExtractor = Wire[RawStudentRelationshipExtractor]

	// Sort members by last name, first name
	implicit val defaultOrderingForMember = Ordering.by { m: Member => (Option(m.lastName), Option(m.firstName), Option(m.universityId)) }

	val apparentUser = viewer.apparentUser

	var additionalAgents: JList[String] = JArrayList()

	var previouslyAllocatedMapping: JMap[Member, JList[Member]] = _ // populated by hidden field in form

	lazy val memberAgentMappingsAfter: Map[Member, Set[StudentMember]] = mapping.asScala.map {
		case (key, value) => (key, value.asScala.toSet)
	}.map {
		case (key, value) => (key, value.collect { case s: StudentMember => s})
	}.toMap // .toMap to make it immutable and avoid type issues

	lazy val studentTutorMapping: Map[StudentMember, TutorInfoForStudent] = {
		val studentTutorsAfterMap = {
			val studentTutorPairs = memberAgentMappingsAfter.map { case (agent, students) =>
				students.map( (_,agent) ).toSeq
			}.flatten

			studentTutorPairs.groupBy(_._1).map {
				case (student, agents) => (student, agents.map(_._2).toSet)
			}
		}

		val studentTutorsBeforeMap = {
			val allMappings = getStudentAgentMappings(department, relationshipType)
			if (spreadsheet)
				// for the spreadsheet, only look at students who appear in the spreadsheet - everyone else should stay the same
				allMappings.filter { case (student, agents) => studentTutorsAfterMap.contains(student) }
			else
				allMappings
		}

		def hasTutorChange(student: StudentMember): Boolean = {
			val oldTutors = studentTutorsBeforeMap.get(student)
			val newTutors = studentTutorsAfterMap.get(student)

			oldTutors != newTutors
		}

		val allStudents = studentTutorsBeforeMap.keySet ++ studentTutorsAfterMap.keySet

		// make data set - only include students with changes
		val result = for (student <- allStudents; if hasTutorChange(student)) yield {
			student -> TutorInfoForStudent(studentTutorsBeforeMap.get(student).toSeq.flatten.toSet, studentTutorsAfterMap.get(student).toSeq.flatten.toSet)
		}

		result.toMap
	}

	// students who have only had tutors removed and no new tutors added
	lazy val studentsWithTutorRemoved = studentTutorMapping.filter { case (student, tutorInfo) =>
		tutorInfo.tutorsBefore != tutorInfo.tutorsAfter && tutorInfo.tutorsAfter.subsetOf(tutorInfo.tutorsBefore)
	}

	// students who have only had tutors added and no tutors removed
	lazy val studentsWithTutorAdded = studentTutorMapping.filter { case (student, tutorInfo) =>
		tutorInfo.tutorsBefore != tutorInfo.tutorsAfter && tutorInfo.tutorsBefore.subsetOf(tutorInfo.tutorsAfter)
	}

	// everyone else
	lazy val studentsWithTutorChanged = studentTutorMapping.filter { case (student, tutorInfo) =>
		val students = studentTutorMapping.keySet -- (studentsWithTutorRemoved.keySet ++ studentsWithTutorAdded.keySet)
		students.contains(student)
	}


	lazy val studentsWithTutorChangedOrAdded = studentsWithTutorChanged ++ studentsWithTutorAdded

	def hasChanges = (studentsWithTutorRemoved.size + studentsWithTutorAdded.size + studentsWithTutorChanged.size) > 0

	var spreadsheet: Boolean = _

	override def onBind(result: BindingResult) {
		super.onBind(result)

		initialiseData()
	}


	def initialiseData() {
		removeBlankAgents()

		// 'mapping' contains a map from agent to students, populated in the form.
		// When new agents are added through the "Add <e.g. personal tutors>" modal, add them to mapping
		additionalAgents.asScala
			.flatMap {
			profileService.getAllMembersWithUserId(_)
		}
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
				additionalAgents.remove
			}
		}
	}

	/**
	 * populate - only called on initial form view.
	 * Populate 'mapping' and 'unallocated' from the database prior to updating.
	 */
	override def populate() {
		populateMapping()
		populateUnallocated()
	}

	// Populate 'mapping' with existing relationships between agents and students, from the database.
	def populateMapping() {
		relationshipService
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
	}

	// Set up unallocated as a list of all students in the department without this type of agent.
	def populateUnallocated() {
		unallocated.clear()
		unallocated.addAll(
			relationshipService
				.listStudentsWithoutRelationship(relationshipType, department)
				.asJavaCollection
		)
	}

	// Purely for use by Freemarker as it can't access map values unless the key is a simple value.
	// Do not modify the returned value!
	def mappingById = mapping.asScala.map {
		case (member, users) => (member.universityId, users)
	}.toMap

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

	// Sort the list of students for each agent by surname, firstname.
	override def sort() {
		// Because sortBy is not an in-place sort, we have to replace the lists entirely.
		// Alternative is Collections.sort or math.Sorting but these would be more code.
		for ((agent, users) <- mapping.asScala) {
			mapping.put(agent, JArrayList(users.asScala.toList.sorted))
		}

		unallocated = JArrayList(unallocated.asScala.toList.sorted)
	}

	/**
	 * applyInternal works out the set of commands needed to remove defunct studentrelationships and the set of
	 * commands needed to edit/add student relationships.  It does this by comparing the existing relationships in the
	 * database and the newly edited relationships that have come through the form in 'mapping'.
	 * It then applies all of those commands and returns the results.
	 *
	 * A note about the member status (or otherwise) of agents:
	 *
	 * 'mapping' stores the mapping of an agent to their students. It maps a Member (the agent) to a JList of Members (the students).
	 * This seems to assume that an agent must be a member. However, StudentRelationship does not restrict agents to members.
	 * It is not currently possible to add non-member agents through the allocate screen,
	 *  but existing relationships with non-member agents are preserved (except where the agent is replaced).
	 */

	final def applyInternal(): Seq[StudentRelationshipChange] = transactional() {
		val endCommands = getEndStudentRelationshipCommands
		val editCommands = getEditStudentRelationshipCommands

		val commandResults = (editCommands ++ endCommands).map { cmd =>
			/*
			 * Defensively code against these defaults changing in future. We do NOT want the
			 * sub-command to send notifications - we'll do that ourselves
			 */
			cmd.notifyStudent = false
			cmd.notifyOldAgents = false
			cmd.notifyNewAgent = false

			val modifiedRelationships = cmd.apply()

			modifiedRelationships.map {
				modifiedRelationship => StudentRelationshipChange(cmd.oldAgents, modifiedRelationship) }
		}

		commandResults.flatten.toSeq
	}

	def getEndStudentRelationshipCommands: Set[EndStudentRelationshipCommand] = {
		val commands = for (
			(student, tutorInfo) <- studentsWithTutorRemoved;
			retiringAgent <- tutorInfo.oldTutors;
			rel <- relationshipService.getCurrentRelationship(relationshipType, student, retiringAgent);
			scd <- student.mostSignificantCourseDetails
		) yield {
			val cmd = new EndStudentRelationshipCommand(rel, viewer)
			cmd.maintenanceMode = this.maintenanceMode // override default in case this is being called in a test
			cmd
		}
		commands.toSet
	}

	def getEditStudentRelationshipCommands: Set[EditStudentRelationshipCommand] = {
		val commands = for (
			(student, tutorInfo) <- studentsWithTutorChangedOrAdded;
			newAgent <- tutorInfo.newTutors;
			scd <- student.mostSignificantCourseDetails
		) yield
		{
			val cmd = new EditStudentRelationshipCommand(scd, relationshipType, tutorInfo.oldTutors.toSeq, viewer, remove=false)
			cmd.agent = newAgent
			cmd.maintenanceMode = this.maintenanceMode // override default in case this is being called in a test
			cmd.relationshipService = relationshipService // override default in case this is being called in a test
			cmd
		}
		commands.toSet
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
		val allocations = relationshipExtractor.readXSSFExcelFile(file.dataStream, relationshipType, Some(department))

		// Put any errors into the BindingResult
		allocations.foreach { case (row, _, errors) =>
			errors.foreach { case (field, code) =>
				result.rejectValue("", code, Array(field, row), "")
			}
		}

		// TAB-2624 Filter erroring rows out of the allocations
		val rawRelationships = allocations
			.filterNot { case (_, _, errors) => errors.nonEmpty }
			.flatMap { case (_, rel, _) => rel }

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


trait FetchesRelationshipMappings {
	var relationshipService: RelationshipService

	def getStudentAgentMappings(department: Department, relationshipType: StudentRelationshipType) : Map[StudentMember, Set[Member]] =
		relationshipService
			.listStudentRelationshipsByDepartment(relationshipType, department) // get all relationships by dept
			.groupBy(_.studentCourseDetails.student) // group into map by student university id
			.map { case (student, rels) =>
				(student, rels.flatMap(_.agentMember).toSet)
			}

}
