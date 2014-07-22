package uk.ac.warwick.tabula.groups.commands.admin.reusable

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{FileAttachment, UserGroup, Department}
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroup, DepartmentSmallGroupSet}
import uk.ac.warwick.tabula.groups.services.docconversion.{AutowiringGroupsExtractorComponent, GroupsExtractorComponent, GroupsExtractor}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._

object AllocateStudentsToDepartmentalSmallGroupsCommand {
	def apply(department: Department, set: DepartmentSmallGroupSet, viewer: CurrentUser) =
		new AllocateStudentsToDepartmentalSmallGroupsCommandInternal(department, set, viewer)
			with ComposableCommand[DepartmentSmallGroupSet]
			with AllocateStudentsToDepartmentalSmallGroupsSorting
			with AllocateStudentsToDepartmentalSmallGroupsFileUploadSupport
			with PopulateAllocateStudentsToDepartmentalSmallGroupsCommand
			with AllocateStudentsToDepartmentalSmallGroupsPermissions
			with AllocateStudentsToDepartmentalSmallGroupsDescription
			with AllocateStudentsToDepartmentalSmallGroupsValidation
			with AllocateStudentsToDepartmentalSmallGroupsViewHelpers
			with AutowiringProfileServiceComponent
			with AutowiringSecurityServiceComponent
			with AutowiringSmallGroupServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringGroupsExtractorComponent
}

class AllocateStudentsToDepartmentalSmallGroupsCommandInternal(val department: Department, val set: DepartmentSmallGroupSet, val viewer: CurrentUser) extends CommandInternal[DepartmentSmallGroupSet] with AllocateStudentsToDepartmentalSmallGroupsCommandState {
	self: GroupsObjects[User, DepartmentSmallGroup] with SmallGroupServiceComponent =>

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

trait AllocateStudentsToDepartmentalSmallGroupsSorting extends GroupsObjects[User, DepartmentSmallGroup] {
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

trait AllocateStudentsToDepartmentalSmallGroupsFileUploadSupport extends GroupsObjectsWithFileUpload[User, DepartmentSmallGroup] {
	self: AllocateStudentsToDepartmentalSmallGroupsCommandState with GroupsExtractorComponent with UserLookupComponent with SmallGroupServiceComponent =>

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
			.groupBy{ x => smallGroupService.getDepartmentSmallGroupById(x.groupId).orNull }
			.mapValues{ values =>
			values.map(item => allocateUsers.find(item.universityId == _.getWarwickId).getOrElse(null)).asJava
		}
	}
}

trait AllocateStudentsToDepartmentalSmallGroupsCommandState {
	def department: Department
	def set: DepartmentSmallGroupSet
	def viewer: CurrentUser
}

trait AllocateStudentsToDepartmentalSmallGroupsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AllocateStudentsToDepartmentalSmallGroupsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, department)
		p.PermissionCheck(Permissions.SmallGroups.Allocate, mandatory(set))
	}
}

trait AllocateStudentsToDepartmentalSmallGroupsDescription extends Describable[DepartmentSmallGroupSet] {
	self: AllocateStudentsToDepartmentalSmallGroupsCommandState =>

	override def describe(d: Description) {
		d.department(set.department).properties("smallGroupSet" -> set.id)
	}

}

trait AllocateStudentsToDepartmentalSmallGroupsValidation extends SelfValidating {
	self: AllocateStudentsToDepartmentalSmallGroupsCommandState with GroupsObjects[User, DepartmentSmallGroup] =>

	override def validate(errors: Errors) {
		// Disallow submitting unrelated Groups
		if (!mapping.asScala.keys.forall( g => set.groups.contains(g) )) {
			errors.reject("smallGroup.allocation.groups.invalid")
		}
	}
}

trait PopulateAllocateStudentsToDepartmentalSmallGroupsCommand extends PopulateOnForm {
	self: AllocateStudentsToDepartmentalSmallGroupsCommandState with GroupsObjects[User, DepartmentSmallGroup] =>

	for (group <- set.groups.asScala) mapping.put(group, JArrayList())

	override def populate() {
		for (group <- set.groups.asScala)
			mapping.put(group, JArrayList(group.students.users.toList))

		unallocated.clear()
		unallocated.addAll(set.unallocatedStudents.asJava)
	}
}

trait AllocateStudentsToDepartmentalSmallGroupsViewHelpers extends MemberCollectionHelper {
	self: AllocateStudentsToDepartmentalSmallGroupsCommandState with GroupsObjects[User, DepartmentSmallGroup] with ProfileServiceComponent with SecurityServiceComponent =>

	// Purely for use by Freemarker as it can't access map values unless the key is a simple value.
	// Do not modify the returned value!
	def mappingById =
		(mapping.asScala
			.filter { case (group, users) => group != null && users != null }
			.map {
			case (group, users) => (group.id, users)
		}).toMap

	// For use by Freemarker to get a simple map of university IDs to Member objects - permissions aware!
	lazy val membersById = loadMembersById

	def loadMembersById = {
		def validUser(user: User) = user.isFoundUser && user.getWarwickId.hasText

		val allUsers = (unallocated.asScala ++ (for ((group, users) <- mapping.asScala) yield users.asScala).flatten)
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