package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{UserLookupService, SmallGroupService, ProfileService, SecurityService}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.userlookup.{AnonymousUser, User}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.groups.services.docconversion.AllocateStudentItem
import uk.ac.warwick.tabula.groups.services.docconversion.GroupsExtractor
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.services.UserLookupService
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}
import uk.ac.warwick.tabula.commands.GroupsObjects
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod.StudentSignUp

class AllocateStudentsToGroupsCommand(val module: Module, val set: SmallGroupSet, val viewer: CurrentUser)
	extends Command[SmallGroupSet]
		with GroupsObjects[User, SmallGroup]
		with SelfValidating
		with BindListener
		with SmallGroupSetCommand
		with NotifiesAffectedGroupMembers {

	mustBeLinked(set, module)
	PermissionCheck(Permissions.SmallGroups.Allocate, set)

	// Sort users by last name, first name
	implicit val defaultOrderingForUser = Ordering.by[User, String] ( user => user.getLastName + ", " + user.getFirstName )

	var userLookup = Wire[UserLookupService]
	val apparentUser = viewer.apparentUser

	var service = Wire[SmallGroupService]
	var profileService = Wire[ProfileService]
	var securityService = Wire[SecurityService]
	var groupsExtractor = Wire.auto[GroupsExtractor]

	for (group <- set.groups.asScala) mapping.put(group, JArrayList())

	var isStudentSignup = set.allocationMethod == StudentSignUp

	// Only called on initial form view
	override def populate() {
		for (group <- set.groups.asScala)
			mapping.put(group, JArrayList(group.students.users.toList))

		unallocated.clear()
		unallocated.addAll(set.unallocatedStudents.asJavaCollection)
	}

	// Purely for use by Freemarker as it can't access map values unless the key is a simple value.
	// Do not modify the returned value!
	override def mappingById = 
		(mapping.asScala
		.filter { case (group, users) => group != null && users != null }
		.map {
			case (group, users) => (group.id, users)
		}).toMap

	// For use by Freemarker to get a simple map of university IDs to Member objects - permissions aware!
	lazy val membersById = loadMembersById

	def loadMembersById = {
		val allUsers = (unallocated.asScala ++ (for ((group, users) <- mapping.asScala) yield users.asScala).flatten)
		val allUniversityIds = allUsers.filter(validUser).map { _.getWarwickId }
		val members = profileService.getAllMembersWithUniversityIds(allUniversityIds)
			.filter(member => securityService.can(viewer, Permissions.Profiles.Read.Core, member))
			.map(member => (member.universityId, member)).toMap
		members
	}

	def allMembersRoutes() = {
		val routes = for {
			member <- membersById.values
			course <- member.mostSignificantCourseDetails}
		yield course.route
		routes.toSeq.sortBy(_.code).distinct
	}

	def allMembersYears(): Seq[JInteger] = {
		val years = for (
			member<-membersById.values;
			course<-member.mostSignificantCourseDetails)
				yield course.latestStudentCourseYearDetails.yearOfStudy
		years.toSeq.distinct.sorted
	}

	// Sort all the lists of users by surname, firstname.
	override def sort() {
		// Because sortBy is not an in-place sort, we have to replace the lists entirely.
		// Alternative is Collections.sort or math.Sorting but these would be more code.
		for ((group, users) <- mapping.asScala) {
			mapping.put(group, JArrayList(users.asScala.toList.filter(validUser).sorted))
		}

		unallocated = JArrayList(unallocated.asScala.toList.filter(validUser).sorted)
	}

	final def applyInternal() = transactional() {
		for ((group, users) <- mapping.asScala) {
			val userGroup = UserGroup.ofUniversityIds
			users.asScala.foreach { user => userGroup.addUser(user.getWarwickId) }
			group._studentsGroup.copyFrom(userGroup)
			service.saveOrUpdate(group)
		}
		set
	}

	def validate(errors: Errors) {
		// Disallow submitting unrelated Groups
		if (!mapping.asScala.keys.forall( g => set.groups.contains(g) )) {
			errors.reject("smallGroup.allocation.groups.invalid")
		}
	}

	private def validUser(user: User) = user.isFoundUser && user.getWarwickId.hasText

	def extractDataFromFile(file: FileAttachment) = {
		val allocations = groupsExtractor.readXSSFExcelFile(file.dataStream)

		// work out users to add to set (all users mentioned in spreadsheet - users currently in set)
		val allocateUsers = allocations.asScala.filter { _.universityId.hasText }.map(x => userLookup.getUserByWarwickUniId(x.universityId)).toSet
		val usersToAddToSet = allocateUsers.filterNot(set.allStudents.toSet)
		for(user <- usersToAddToSet) set.members.add(user)

		allocations.asScala
			.filter(_.groupId != null)
			.groupBy{ x => service.getSmallGroupById(x.groupId).orNull }
			.mapValues{ values =>
				values.map(item => allocateUsers.find(item.universityId == _.getWarwickId).getOrElse(null)).asJava
			}
	}

	def describe(d: Description) = d.smallGroupSet(set)

}
