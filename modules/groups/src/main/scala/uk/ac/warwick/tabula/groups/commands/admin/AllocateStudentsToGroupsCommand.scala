package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.SmallGroupService
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.groups.services.docconversion.AllocateStudentItem
import uk.ac.warwick.tabula.groups.services.docconversion.GroupsExtractor
import uk.ac.warwick.tabula.data.model.FileAttachment
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.services.UserLookupService
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}


class AllocateStudentsToGroupsCommand(val module: Module, val set: SmallGroupSet, viewer: CurrentUser) 
	extends Command[SmallGroupSet] with SelfValidating with BindListener {
	
	mustBeLinked(set, module)
	PermissionCheck(Permissions.SmallGroups.Allocate, set)
	
	// Sort users by last name, first name
	implicit val defaultOrderingForUser = Ordering.by[User, String] ( user => user.getLastName + ", " + user.getFirstName )
	
	var service = Wire[SmallGroupService]
	var profileService = Wire[ProfileService]
	var securityService = Wire[SecurityService]
	var groupsExtractor = Wire.auto[GroupsExtractor]
	var userLookup = Wire[UserLookupService]
	
	var file: UploadedFile = new UploadedFile
	var allocateStudent: JList[AllocateStudentItem] = LazyLists.simpleFactory()

	private def filenameOf(path: String) = new java.io.File(path).getName
	
	/** Mapping from small groups to an ArrayList containing users. */
	var mapping = JMap[SmallGroup, JList[User]]()
	var unallocated: JList[User] = JArrayList()
	
	for (group <- set.groups.asScala) mapping.put(group, JArrayList())
	
	// Only called on initial form view
	def populate() {
		for (group <- set.groups.asScala)
			mapping.put(group, JArrayList(group.students.users.toList))
			
		unallocated.clear()
		unallocated.addAll(set.unallocatedStudents.asJavaCollection)
	}
	

	// Purely for use by Freemarker as it can't access map values unless the key is a simple value.
	// Do not modify the returned value!
	def mappingById = mapping.asScala.map {
		case (group, users) => (group.id, users)
	}
	
	// For use by Freemarker to get a simple map of university IDs to Member objects - permissions aware!
	def membersById = {
		val allUsers = (unallocated.asScala ++ (for ((group, users) <- mapping.asScala) yield users.asScala).flatten)
		val allUniversityIds = allUsers.filter(validUser).map { _.getWarwickId }
		profileService.getAllMembersWithUniversityIds(allUniversityIds)
			.filter(member => securityService.can(viewer, Permissions.Profiles.Read.Core, member))
			.map(member => (member.universityId, member)).toMap
	}
	
	// Sort all the lists of users by surname, firstname.
	def sort() {
		// Because sortBy is not an in-place sort, we have to replace the lists entirely.
		// Alternative is Collections.sort or math.Sorting but these would be more code.
		for ((group, users) <- mapping.asScala) {
			mapping.put(group, JArrayList(users.asScala.toList.filter(validUser).sorted))
		}
		
		unallocated = JArrayList(unallocated.asScala.toList.filter(validUser).sorted)
	}
	
	final def applyInternal() = transactional() {
		for ((group, users) <- mapping.asScala) {
			val userGroup = UserGroup.emptyUniversityIds
			users.asScala.foreach { user => userGroup.addUser(user.getWarwickId) }
			group.students.copyFrom(userGroup)
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
	
	override def onBind(result:BindingResult) {
		transactional() {
			file.onBind(result)
			if (!file.attached.isEmpty()) {
				processFiles(file.attached.asScala)
			}

			def processFiles(files: Seq[FileAttachment]) {
				for (file <- files.filter(_.hasData)) {
					allocateStudent addAll groupsExtractor.readXSSFExcelFile(file.dataStream)
				}

				val grouped = allocateStudent.asScala.filter(_.groupId!=null)
						.groupBy{ x => service.getSmallGroupById(x.groupId).orNull }
						.mapValues{ values => 
							values.map(item => userLookup.getUserByWarwickUniId(item.universityId)).asJava
						}
				
				mapping.clear()
				mapping.putAll( (grouped - null).asJava )
					  
			}
		}
	}
	
	def describe(d: Description) = d.smallGroupSet(set)

}