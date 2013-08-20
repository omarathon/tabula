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
import uk.ac.warwick.tabula.profiles.web.controllers.tutor.EditTutorCommand
import uk.ac.warwick.tabula.commands.GroupsObjects
import uk.ac.warwick.tabula.data.model.FileAttachment

class AllocateStudentsToTutorsCommand(val department: Department, val viewer: CurrentUser)
	extends Command[Seq[Option[StudentRelationship]]] 
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
		routes.toSeq.distinct
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
		val addedRelationships = (for ((tutor, students) <- mapping.asScala; student <- students.asScala) yield {
			student.mostSignificantCourseDetails.flatMap { studentCourseDetails => 
				val cmd = new EditTutorCommand(studentCourseDetails, service.findCurrentRelationships(RelationshipType.PersonalTutor, studentCourseDetails.sprCode).headOption.flatMap { _.agentMember }, viewer, false)
				cmd.tutor = tutor
				
				cmd.apply()
			}
		}).toSeq
		
		val removedRelationships = (unallocated.asScala.flatMap { student =>
			student.mostSignificantCourseDetails.map { studentCourseDetails =>
				val rels = service.findCurrentRelationships(RelationshipType.PersonalTutor, studentCourseDetails.sprCode)
				val tutors = rels.flatMap { _.agentMember }
				
				val changedRelationships = tutors.map { tutor =>
					val cmd = new EditTutorCommand(studentCourseDetails, Some(tutor), viewer, true)
					cmd.tutor = tutor
					cmd.apply()
				}
				
				changedRelationships
			}
		}).toSeq.flatten
		
		addedRelationships ++ removedRelationships
	}
	
	def extractDataFromFile(file: FileAttachment) = ??? // TODO

	def validate(errors: Errors) {
		// Nothing to do
	}
	
	def describe(d: Description) = d.department(department)

}
