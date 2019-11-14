package uk.ac.warwick.tabula.commands.groups.admin.reusable

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.admin.AllocateStudentsToGroupsViewHelpers
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroup, DepartmentSmallGroupSet}
import uk.ac.warwick.tabula.data.model.{Department, FileAttachment, UserGroup}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.groups.docconversion.{AutowiringGroupsExtractorComponent, GroupsExtractor, GroupsExtractorComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

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
      with AllocateStudentsToGroupsViewHelpers[DepartmentSmallGroup]
      with AutowiringProfileServiceComponent
      with AutowiringSmallGroupServiceComponent
      with AutowiringUserLookupComponent
      with AutowiringGroupsExtractorComponent
}

class AllocateStudentsToDepartmentalSmallGroupsCommandInternal(val department: Department, val set: DepartmentSmallGroupSet, val viewer: CurrentUser)
  extends CommandInternal[DepartmentSmallGroupSet] with AllocateStudentsToDepartmentalSmallGroupsCommandState with TaskBenchmarking {

  self: GroupsObjects[User, DepartmentSmallGroup] with SmallGroupServiceComponent =>

  override def applyInternal(): DepartmentSmallGroupSet = transactional() {

    lazy val groupEventOccurrences = benchmarkTask("Get all small group event occurrences for the groups") {
      mapping.asScala.keys.flatMap(_.linkedGroups.asScala).map(group => group -> smallGroupService.findAttendanceByGroup(group)).toMap
    }

    for ((group, users) <- mapping.asScala) {
      val userGroup = UserGroup.ofUniversityIds
      users.asScala.foreach { user =>
        userGroup.addUserId(user.getWarwickId)
        // if the user is being added to this group then back fill any attendance for past events they won't have been expected to attend
        if(!group.students.includesUser(user)) {
          group.linkedGroups.asScala.foreach(g =>
            smallGroupService.backFillAttendance(user.getWarwickId, groupEventOccurrences(g), viewer)
          )
        }
      }
      group.students.copyFrom(userGroup)
      smallGroupService.saveOrUpdate(group)
    }
    set
  }
}

trait AllocateStudentsToDepartmentalSmallGroupsSorting extends GroupsObjects[User, DepartmentSmallGroup] {
  // Sort users by last name, first name
  implicit val defaultOrderingForUser: Ordering[User] = Ordering.by { user: User => (user.getLastName, user.getFirstName, user.getUserId) }

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

    if (invalidFiles.nonEmpty) {
      if (invalidFiles.size == 1) result.rejectValue("file", "file.wrongtype.one", Array(invalidFiles.mkString(""), GroupsExtractor.AcceptedFileExtensions.mkString(", ")), "")
      else result.rejectValue("", "file.wrongtype", Array(invalidFiles.mkString(", "), GroupsExtractor.AcceptedFileExtensions.mkString(", ")), "")
    }
  }

  override def extractDataFromFile(file: FileAttachment, result: BindingResult): Map[DepartmentSmallGroup, JList[User]] = {
    val allocations = groupsExtractor.readXSSFExcelFile(file.asByteSource.openStream())

    // work out users to add to set (all users mentioned in spreadsheet - users currently in set)
    val allocateUsers = userLookup.usersByWarwickUniIds(allocations.asScala.toSeq.map(_.universityId).filter(_.hasText)).values.toSet
    val usersToAddToSet = allocateUsers.filterNot(set.allStudents.toSet)
    for (user <- usersToAddToSet) set.members.add(user)

    allocations.asScala
      .filter(_.groupId != null)
      .groupBy { x => smallGroupService.getDepartmentSmallGroupById(x.groupId).orNull }
      .view
      .mapValues { values =>
        values.map(item => allocateUsers.find(item.universityId == _.getWarwickId).orNull).asJava
      }
      .toMap
  }
}

trait AllocateStudentsToDepartmentalSmallGroupsCommandState extends HasAcademicYear {
  def department: Department

  def set: DepartmentSmallGroupSet

  def viewer: CurrentUser

  override def academicYear: AcademicYear = set.academicYear

  var sortedDeptGroups: JList[DepartmentSmallGroup] = JArrayList()
}

trait AllocateStudentsToDepartmentalSmallGroupsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: AllocateStudentsToDepartmentalSmallGroupsCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    mustBeLinked(set, department)
    p.PermissionCheck(Permissions.SmallGroups.Allocate, mandatory(set))
  }
}

trait AllocateStudentsToDepartmentalSmallGroupsDescription extends Describable[DepartmentSmallGroupSet] {
  self: AllocateStudentsToDepartmentalSmallGroupsCommandState =>

  override def describe(d: Description): Unit =
    d.departmentSmallGroupSet(set)

}

trait AllocateStudentsToDepartmentalSmallGroupsValidation extends SelfValidating {
  self: AllocateStudentsToDepartmentalSmallGroupsCommandState with GroupsObjects[User, DepartmentSmallGroup] =>

  override def validate(errors: Errors): Unit = {
    // Disallow submitting unrelated Groups
    if (!mapping.asScala.keys.forall(g => set.groups.contains(g))) {
      errors.reject("smallGroup.allocation.groups.invalid")
    }
  }
}

trait PopulateAllocateStudentsToDepartmentalSmallGroupsCommand extends PopulateOnForm {
  self: AllocateStudentsToDepartmentalSmallGroupsCommandState with GroupsObjects[User, DepartmentSmallGroup] =>

  for (group <- set.groups.asScala) mapping.put(group, JArrayList())

  override def populate(): Unit = {
    for (group <- set.groups.asScala)
      mapping.put(group, JArrayList(group.students.users.toList))

    sortedDeptGroups = set.groups.asScala.sorted.asJava
    unallocated.clear()
    unallocated.addAll(set.unallocatedStudents.asJava)
  }
}
