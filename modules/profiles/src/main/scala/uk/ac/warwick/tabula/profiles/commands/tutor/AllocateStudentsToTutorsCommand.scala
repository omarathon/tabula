package uk.ac.warwick.tabula.profiles.commands.tutor

import scala.collection.JavaConverters._
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.model.RelationshipType
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.commands.GroupsObjects
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.userlookup.User

class AllocateStudentsToTutorsCommand(val department: Department, val viewer: CurrentUser)
	extends Command[Seq[PersonalTutorChange]] 
		with GroupsObjects[Member, Member] 
		with SelfValidating 
		with BindListener 
		with DepartmentCommand 
		with NotifiesAffectedStudents {
	
	PermissionCheck(Permissions.Profiles.PersonalTutor.Update, mandatory(department))

	// throw this request out if personal tutors can't be edited in Tabula for this department
	if (!department.canEditPersonalTutors) {
		logger.info(
			"Denying access to AllocateStudentsToTutorsCommand since department %s has a personal tutor source setting of %s."
			.format(department.name, department.personalTutorSource)
		)
		throw new ItemNotFoundException()
	}
	
	// Sort members by last name, first name
	implicit val defaultOrderingForMember = Ordering.by[Member, String] ( user => user.lastName + ", " + user.firstName )

	val apparentUser = viewer.apparentUser

	var service = Wire[RelationshipService]
	var profileService = Wire[ProfileService]
	var securityService = Wire[SecurityService]
	
	var additionalTutors: JList[String] = JArrayList()
	
	override def onBind(result: BindingResult) {
		super.onBind(result)
		
		// Find all empty textboxes for tutors and remove them - otherwise we end up with a never ending list of empties
		val indexesToRemove = additionalTutors.asScala.zipWithIndex.flatMap { case (tutor, index) =>
			if (!tutor.hasText) Some(index)
			else None
		}
		
		// We reverse because removing from the back is better
		indexesToRemove.reverse.foreach { additionalTutors.remove(_) }
		
		additionalTutors.asScala
			.flatMap { profileService.getMemberByUserId(_) }
			.foreach { member => 
				if (!mapping.containsKey(member)) mapping.put(member, JArrayList())
			}
	}

	// Only called on initial form view
	override def populate() {
		def studentRelationshipToMember(rel: StudentRelationship) = profileService.getStudentBySprCode(rel.targetSprCode)
		
		// get all tutor/tutee relationships by dept
		service
			.listStudentRelationshipsByDepartment(RelationshipType.PersonalTutor, department)
			.groupBy(_.agent) // group into map by tutor id
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
				.listStudentsWithoutRelationship(RelationshipType.PersonalTutor, department)
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
			(unallocated.asScala ++ (for ((tutor, students) <- mapping.asScala) yield tutor +: students.asScala).flatten)
			.filter(member => securityService.can(viewer, Permissions.Profiles.Read.Core, member))
			.map(member => (member.universityId, member)).toMap
		members
	}

	def allMembersRoutes = {
		val routes = for {
			member <- membersById.values
			course <- member.mostSignificantCourseDetails}
		yield course.route
		routes.toSeq.sortBy(_.code).distinct
	}

	def allMembersYears: Seq[JInteger] = {
		val years = for (
			member <- membersById.values;
			course <- member.mostSignificantCourseDetails) yield course.latestStudentCourseYearDetails.yearOfStudy
		years.toSeq.distinct.sorted
	}

	// Sort all the lists of users by surname, firstname.
	override def sort() {
		// Because sortBy is not an in-place sort, we have to replace the lists entirely.
		// Alternative is Collections.sort or math.Sorting but these would be more code.
		for ((tutor, users) <- mapping.asScala) {
			mapping.put(tutor, JArrayList(users.asScala.toList.sorted))
		}
		
		unallocated = JArrayList(unallocated.asScala.toList.sorted)
	}
	
	final def applyInternal() = transactional() {
		val addCommands = (for ((tutor, students) <- mapping.asScala; student <- students.asScala) yield {
			student.mostSignificantCourseDetails.map { studentCourseDetails => 
				val cmd = new EditTutorCommand(studentCourseDetails, service.findCurrentRelationships(RelationshipType.PersonalTutor, studentCourseDetails.sprCode).headOption.flatMap { _.agentMember }, viewer, false)
				cmd.tutor = tutor
				cmd
			}
		}).toSeq.flatten
		
		val removeCommands = (unallocated.asScala.flatMap { student =>
			student.mostSignificantCourseDetails.map { studentCourseDetails =>
				val rels = service.findCurrentRelationships(RelationshipType.PersonalTutor, studentCourseDetails.sprCode)
				val tutors = rels.flatMap { _.agentMember }
				
				tutors.map { tutor =>
					val cmd = new EditTutorCommand(studentCourseDetails, Some(tutor), viewer, true)
					cmd.tutor = tutor
					cmd
				}
			}
		}).toSeq.flatten
		
		(addCommands ++ removeCommands).map { cmd =>
			/*
			 * Defensively code against these defaults changing in future. We do NOT want the 
			 * sub-command to send notifications - we'll do that ourselves
			 */ 
			cmd.notifyTutee = false
			cmd.notifyOldTutor = false
			cmd.notifyNewTutor = false
			
			cmd.apply().map { modifiedRelationship => PersonalTutorChange(cmd.currentTutor, modifiedRelationship) }
		}.flatten
	}
	
	def extractDataFromFile(file: FileAttachment) = ??? // TODO

	def validate(errors: Errors) {
		// Nothing to do
	}
	
	def describe(d: Description) = d.department(department)

}

case class PersonalTutorChange(
	oldTutor: Option[Member],
	modifiedRelationship: StudentRelationship
)
