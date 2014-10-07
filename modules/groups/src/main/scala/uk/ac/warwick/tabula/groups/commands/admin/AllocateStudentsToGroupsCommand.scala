package uk.ac.warwick.tabula.groups.commands.admin

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.groups.services.docconversion.{AutowiringGroupsExtractorComponent, GroupsExtractorComponent, GroupsExtractor}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._

object AllocateStudentsToGroupsCommand {
	def apply(module: Module, set: SmallGroupSet, viewer: CurrentUser) =
		new AllocateStudentsToGroupsCommandInternal(module, set, viewer)
			with ComposableCommand[SmallGroupSet]
			with AllocateStudentsToGroupsSorting
			with AllocateStudentsToGroupsFileUploadSupport
			with PopulateAllocateStudentsToGroupsCommand
			with AllocateStudentsToGroupsPermissions
			with AllocateStudentsToGroupsDescription
			with AllocateStudentsToGroupsValidation
			with AllocateStudentsToGroupsViewHelpers
			with NotifiesAffectedGroupMembers
			with AutowiringProfileServiceComponent
			with AutowiringSecurityServiceComponent
			with AutowiringSmallGroupServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringGroupsExtractorComponent
}

class AllocateStudentsToGroupsCommandInternal(val module: Module, val set: SmallGroupSet, val viewer: CurrentUser) extends CommandInternal[SmallGroupSet] with AllocateStudentsToGroupsCommandState {
	self: GroupsObjects[User, SmallGroup] with SmallGroupServiceComponent =>

	override def applyInternal() = transactional() {
		for ((group, users) <- mapping.asScala) {
			val userGroup = UserGroup.ofUniversityIds
			users.asScala.foreach { user => userGroup.addUserId(user.getWarwickId) }
			group.students.copyFrom(userGroup)
			smallGroupService.saveOrUpdate(group)
		}
		set
	}
}

trait AllocateStudentsToGroupsSorting extends GroupsObjects[User, SmallGroup] {
	// Sort users by last name, first name
	implicit val defaultOrderingForUser = Ordering.by { user: User => (user.getLastName, user.getFirstName, user.getUserId) }

	// Sort all the lists of users by surname, firstname.
	override def sort() {
		def validUser(user: User) = user.isFoundUser && user.getWarwickId.hasText

		// Because sortBy is not an in-place sort, we have to replace the lists entirely.
		// Alternative is Collections.sort or math.Sorting but these would be more code.
		for ((group, users) <- mapping.asScala) {
			mapping.put(group, JArrayList(users.asScala.toList.filter(validUser).sorted))
		}

		unallocated = JArrayList(unallocated.asScala.toList.filter(validUser).sorted)
	}
}

trait AllocateStudentsToGroupsFileUploadSupport extends GroupsObjectsWithFileUpload[User, SmallGroup] {
	self: AllocateStudentsToGroupsCommandState with GroupsExtractorComponent with UserLookupComponent with SmallGroupServiceComponent =>

	override def validateUploadedFile(result: BindingResult) {
		val fileNames = file.fileNames map (_.toLowerCase)
		val invalidFiles = fileNames.filter(s => !GroupsExtractor.AcceptedFileExtensions.exists(s.endsWith))

		if (invalidFiles.size > 0) {
			if (invalidFiles.size == 1) result.rejectValue("file", "file.wrongtype.one", Array(invalidFiles.mkString("")), "")
			else result.rejectValue("", "file.wrongtype", Array(invalidFiles.mkString(", ")), "")
		}
	}

	override def extractDataFromFile(file: FileAttachment, result: BindingResult) = {
		val allocations = groupsExtractor.readXSSFExcelFile(file.dataStream)

		// work out users to add to set (all users mentioned in spreadsheet - users currently in set)
		val allocateUsers = userLookup.getUsersByWarwickUniIds(allocations.asScala.map { _.universityId }.filter { _.hasText }).values.toSet
		val usersToAddToSet = allocateUsers.filterNot(set.allStudents.toSet)
		for(user <- usersToAddToSet) set.members.add(user)

		allocations.asScala
			.filter(_.groupId != null)
			.groupBy{ x => smallGroupService.getSmallGroupById(x.groupId).orNull }
			.mapValues{ values =>
			values.map(item => allocateUsers.find(item.universityId == _.getWarwickId).orNull).asJava
		}
	}
}

trait AllocateStudentsToGroupsCommandState extends SmallGroupSetCommand {
	def module: Module
	def set: SmallGroupSet
	def viewer: CurrentUser
	def apparentUser = viewer.apparentUser

	def isStudentSignup = set.allocationMethod == SmallGroupAllocationMethod.StudentSignUp
}

trait AllocateStudentsToGroupsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AllocateStudentsToGroupsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, module)
		p.PermissionCheck(Permissions.SmallGroups.Allocate, mandatory(set))
	}
}

trait AllocateStudentsToGroupsDescription extends Describable[SmallGroupSet] {
	self: AllocateStudentsToGroupsCommandState =>

	override def describe(d: Description) {
		d.smallGroupSet(set)
	}

	override def describeResult(d: Description, set: SmallGroupSet) = {
		d.property("allocation", set.groups.asScala.map(g => g.id -> g.students.users.map(_.getUserId)))
	}

}

trait AllocateStudentsToGroupsValidation extends SelfValidating {
	self: AllocateStudentsToGroupsCommandState with GroupsObjects[User, SmallGroup] =>

	override def validate(errors: Errors) {
		// Disallow submitting unrelated Groups
		if (!mapping.asScala.keys.forall( g => set.groups.contains(g) )) {
			errors.reject("smallGroup.allocation.groups.invalid")
		}
	}
}

trait PopulateAllocateStudentsToGroupsCommand extends PopulateOnForm {
	self: AllocateStudentsToGroupsCommandState with GroupsObjects[User, SmallGroup] with ProfileServiceComponent =>

	for (group <- set.groups.asScala) mapping.put(group, JArrayList())

	override def populate() {
		for (group <- set.groups.asScala)
			mapping.put(group, JArrayList(group.students.users.toList))

		unallocated.clear()
		unallocated.addAll(removePermanentlyWithdrawn(set.unallocatedStudents).asJava)
	}

	def removePermanentlyWithdrawn(users: Seq[User]) = {
		val members: Seq[Member] = users.flatMap(usr => profileService.getMemberByUser(usr))
		val membersFiltered: Seq[Member] = members filter {
			case (student: StudentMember) => !student.permanentlyWithdrawn
			case (member: Member) => true
		}
		membersFiltered.map { mem => mem.asSsoUser}
	}
}

trait AllocateStudentsToGroupsViewHelpers extends MemberCollectionHelper {
	self: AllocateStudentsToGroupsCommandState with GroupsObjects[User, SmallGroup] with ProfileServiceComponent with SecurityServiceComponent =>

	// Purely for use by Freemarker as it can't access map values unless the key is a simple value.
	// Do not modify the returned value!
	def mappingById =
		mapping.asScala
			.filter { case (group, users) => group != null && users != null}
			.map {
			case (group, users) => (group.id, users)
		}.toMap

	// For use by Freemarker to get a simple map of university IDs to Member objects - permissions aware!
	lazy val membersById = loadMembersById

	def loadMembersById = {
		def validUser(user: User) = user.isFoundUser && user.getWarwickId.hasText

		val allUsers = unallocated.asScala ++ (for ((group, users) <- mapping.asScala) yield users.asScala).flatten
		val allUniversityIds = allUsers.filter(validUser).map { _.getWarwickId }
		val members = profileService.getAllMembersWithUniversityIds(allUniversityIds)
			.filter(member => securityService.can(viewer, Permissions.Profiles.Read.Core, member))
			.map(member => (member.universityId, member)).toMap
		members
	}

	def allMembersRoutes() = {
		allMembersRoutesSorted(membersById.values)
	}

	def allMembersYears(): Seq[JInteger] = {
		allMembersYears(membersById.values)
	}
}