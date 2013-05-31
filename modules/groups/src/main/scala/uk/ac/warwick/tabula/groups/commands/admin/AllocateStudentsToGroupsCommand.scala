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

class AllocateStudentsToGroupsCommand(val module: Module, val set: SmallGroupSet) 
	extends Command[SmallGroupSet] with SelfValidating {
	
	mustBeLinked(set, module)
	PermissionCheck(Permissions.SmallGroups.Allocate, set)
	
	// Sort users by last name, first name
	implicit val defaultOrderingForUser = Ordering.by[User, String] ( user => user.getLastName + ", " + user.getFirstName )
	
	var service = Wire[SmallGroupService]
	
	/** Mapping from departments to an ArrayList containing user IDs. */
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
		// TODO
	}

	private def validUser(user: User) = user.isFoundUser && user.getWarwickId.hasText
	
	def describe(d: Description) = d.smallGroupSet(set)

}