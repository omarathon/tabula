package uk.ac.warwick.tabula.profiles.commands.relationships

import scala.collection.JavaConverters._
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.commands.{GroupsObjectsWithFileUpload, MemberCollectionHelper, SelfValidating, Command, Description}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.Transactions._
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

	type AgentMap = Map[Member, Set[StudentMember]]

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

	var memberAgentMappingsBefore: AgentMap = _
	var studentsBefore: Set[StudentMember] = _

	var memberAgentMappingsAfter: AgentMap = _
	var studentsAfter: Set[StudentMember] = _

	var studentsWithTutorRemoved: Set[StudentMember] = _
	var studentsWithTutorAdded: Set[StudentMember] = _
	var studentsWithTutorChanged: Set[StudentMember] = _

	var studentsWithTutorChangedOrAdded: Set[StudentMember] = _

	var newOrChangedMapping: AgentMap = _

	var hasChanges: Boolean = _

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

		memberAgentMappingsAfter = mapping.asScala.map {
			case (key, value) => (key, value.asScala.toSet)
		}.map {
			case (key, value) => (key, value.collect { case s: StudentMember => s})
		}.toMap // .toMap to make it immutable and avoid type issues

		studentsAfter = memberAgentMappingsAfter.values.flatten.toSet

		memberAgentMappingsBefore = getMemberAgentMappingsBefore
		studentsBefore = memberAgentMappingsBefore.values.flatten.toSet

		studentsWithTutorRemoved = studentsBefore.filterNot(memberAgentMappingsAfter.values.flatten.toSet)
		studentsWithTutorAdded = studentsAfter.filterNot(memberAgentMappingsBefore.values.flatten.toSet)
		studentsWithTutorChanged = studentsBefore.intersect(studentsAfter).filter(isTutorChanged)

		studentsWithTutorChangedOrAdded = studentsWithTutorChanged ++ studentsWithTutorAdded

		newOrChangedMapping = getNewOrChangedMapping

		hasChanges = (studentsWithTutorRemoved.size + studentsWithTutorAdded.size + studentsWithTutorChanged.size) > 0

		def getNewOrChangedMapping: AgentMap = {
			val mappingsNewOrChangedMutable = scala.collection.mutable.Map[Member, Set[StudentMember]]()

			for (agentMap <- memberAgentMappingsAfter) {
				val agent = agentMap._1
				val studentsForAgentAfter = agentMap._2
				val studentsForAgentBefore = if (memberAgentMappingsBefore.contains(agent)) memberAgentMappingsBefore(agent) else Set[StudentMember]()
				val changedStudentsForAgent = (studentsForAgentBefore ++ studentsForAgentAfter) -- studentsForAgentBefore.intersect(studentsForAgentAfter)
				if (!changedStudentsForAgent.isEmpty) {
					mappingsNewOrChangedMutable(agent) = changedStudentsForAgent
				}
			}
			mappingsNewOrChangedMutable.toMap
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
	}

	// Set up unallocated as a list of all students in the department without this type of agent.
	def populateUnallocated() {
		unallocated.clear()
		unallocated.addAll(
			service
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
		val memberAgentsBefore = memberAgentMappingsBefore.keySet.toSet // .toSet to make it immutable and avoid type issues
		val memberAgentsAfter = memberAgentMappingsAfter.keySet.toSet // .toSet to make it immutable and avoid type issues

		val newMemberAgents = memberAgentsAfter.filterNot(memberAgentsBefore)
		val droppedMemberAgents = memberAgentsBefore.filterNot(memberAgentsAfter)
		val changedMemberAgents = memberAgentsAfter.intersect(memberAgentsBefore).filterNot(agent => memberAgentMappingsBefore.get(agent).equals(memberAgentMappingsAfter.get(agent)))

		val removeCommandsForDroppedAgents = getRemoveCommandsForDroppedAgents(droppedMemberAgents)
		val removeCommandsForChangedAgents = getRemoveCommandsForChangedAgents(memberAgentMappingsBefore, memberAgentMappingsAfter, changedMemberAgents)
		val editCommands = getEditStudentCommands(memberAgentMappingsBefore, memberAgentMappingsAfter, newMemberAgents ++ changedMemberAgents)

		val commandResults = (editCommands ++ removeCommandsForChangedAgents ++ removeCommandsForDroppedAgents).map { cmd =>
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

	def isTutorChanged(student: StudentMember): Boolean = {
		val tutorsBefore = memberAgentMappingsBefore.filter(agentMap => agentMap._2.contains(student)).keySet
		val tutorsAfter = memberAgentMappingsAfter.filter(agentMap => agentMap._2.contains(student)).keySet
		tutorsBefore != tutorsAfter
	}

	def getMemberAgentMappingsBefore: AgentMap = {
		if (spreadsheet) {
			// spreadsheet upload - we only want the students who are included in the input data set (studentsAfter) to be included in comparisons
			assert(studentsAfter != null)
			val mappingsBeforeMutable = scala.collection.mutable.Map[Member, Set[StudentMember]]()

			for (agentMap <- getMemberAgentMappingsFromDatabase) {
				val agent = agentMap._1
				val students = agentMap._2 match {
					case studentSet: Set[StudentMember] => studentSet.filter(studentsAfter.contains(_))
					case _ => Set[StudentMember]()
				}

				if (!students.isEmpty) mappingsBeforeMutable(agent) = students
			}
			mappingsBeforeMutable.toMap
		}
		else {
			getMemberAgentMappingsFromDatabase
		}
	}

	def getMemberAgentMappingsFromDatabase: AgentMap = {
		val memberAgentMappingsFromDb = scala.collection.mutable.Map[Member, Set[StudentMember]]()

		service
			.listStudentRelationshipsByDepartment(relationshipType, department) // get all relationships by dept
			.groupBy(_.agent) // group into map by agent university id
			.foreach { case (agent, students) =>
			if (agent.forall(_.isDigit)) {
				profileService.getMemberByUniversityId(agent) match {
					case Some(member) =>
						memberAgentMappingsFromDb(member) = students.flatMap(_.studentMember).toSet
					case _ => // the agent isn't a member
				}
			}
		}
		memberAgentMappingsFromDb.toMap // convert to immutable map
	}

	// get the commands needed to remove the relationships of this type for dropped agents:
	def getRemoveCommandsForDroppedAgents(droppedAgents: Set[Member]): Set[EndStudentRelationshipCommand] = {
		for (
			agent <- droppedAgents;
			relationship <- service.listStudentRelationshipsWithMember(relationshipType, agent)
		) yield {
			val cmd = new EndStudentRelationshipCommand(relationship, viewer)
			cmd.maintenanceMode = this.maintenanceMode
			cmd.relationshipService = service
			cmd
		}
	}

	/**
	 * 	getEditStudentCommands
	 *
	 * for each member of agentsToEdit, work out which of their relationships are new and
	 * get the commands needed to add or replace them
	 *
	 */
	def getEditStudentCommands(memberAgentMappingsBefore: AgentMap,
															memberAgentMappingsAfter: AgentMap,
															agentsToEdit: Set[Member]): Set[EditStudentRelationshipCommand] = {

		def getStudentsForAgent(agentMap: AgentMap, agent: Member) = agentMap.get(agent) match {
			case Some(students: Set[StudentMember]) => students
			case _ => Set[StudentMember]()
		}

		(for (agentToEdit <- agentsToEdit) yield {

			// get the old and new student sets for the agent
			val studentsBefore = getStudentsForAgent(memberAgentMappingsBefore, agentToEdit)
			val studentsAfter = getStudentsForAgent(memberAgentMappingsAfter, agentToEdit)

			val newStudentMembersForAgent = studentsAfter.filterNot(studentsBefore)

			// and for each, get a command to edit the relationship for that student to change it to the agent
			newStudentMembersForAgent.flatMap (	stu => {
				stu.mostSignificantCourseDetails.map ( scd => {
					val existingAgentsForStudent = service.findCurrentRelationships(relationshipType, scd).flatMap {
						_.agentMember
					}

					val cmd = new EditStudentRelationshipCommand(scd, relationshipType, existingAgentsForStudent, viewer, false)
					cmd.agent = agentToEdit
					cmd.maintenanceMode = this.maintenanceMode // override default in case this is being called in a test
					cmd.relationshipService = service // override default in case this is being called in a test
					cmd
				})
			})
		}).flatten
	}

	/**
	 * getRemoveCommandsForChangedAgents: get the commands needed to remove the relationships for students who
	 * are no longer attached to any tutor tutors with changed tutee sets
	 */
	def getRemoveCommandsForChangedAgents(memberAgentMappingsBefore: AgentMap,
																				memberAgentMappingsAfter: AgentMap,
																				changedMemberAgents: Set[Member]): Set[EndStudentRelationshipCommand] = {

		val commands = for (agent <- changedMemberAgents) yield {
			val studentsForAgentBefore = memberAgentMappingsBefore.getOrElse(agent, Set[StudentMember]())

			// drop those students who don't feature at all in the after mapping
			val studentsToDrop = studentsForAgentBefore.filterNot(memberAgentMappingsAfter.values.flatten.toSet)

			studentsToDrop.flatMap ( stu => {
				stu.mostSignificantCourseDetails.map (scd => {

					// The next line assumes that there is a relationship for this scd and agent and that the agent is a member.
					// These are fair assumptions since the data is derived from 'mapping' which is the current state of the db
					// and which stores agents as staff members.
					val rel = service.findCurrentRelationships(relationshipType, scd).filter(_.agentMember.getOrElse(throw new ItemNotFoundException).equals(agent)).head

					val cmd = new EndStudentRelationshipCommand(rel, viewer)
					cmd.maintenanceMode = this.maintenanceMode
					cmd.relationshipService = service
					cmd
				})
			})
		}
		commands.flatten
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

