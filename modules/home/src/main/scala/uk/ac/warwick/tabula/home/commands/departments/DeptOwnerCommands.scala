package uk.ac.warwick.tabula.home.commands.departments

import scala.reflect.BeanProperty
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.Department
import org.hibernate.validator.constraints.NotEmpty
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions._


class AddDeptOwnerCommand(val department: Department) extends Command[Unit] with Daoisms {
	
	PermissionCheck(Permissions.Department.ManagePermissions(), department)

	def getUsercodes: Seq[String] = department.owners.members

	@NotEmpty
	@BeanProperty var usercode: String = _

	override def applyInternal() = transactional() {
		department.addOwner(usercode)
	}

	override def describe(d: Description) = d.properties(
		"department" -> department.code,
		"usercode" -> usercode)
}

class RemoveDeptOwnerCommand(val department: Department) extends Command[Unit] with Daoisms {
	
	PermissionCheck(Permissions.Department.ManagePermissions(), department)

	def getUsercodes: Seq[String] = department.owners.members

	@NotEmpty
	@BeanProperty var usercode: String = _

	override def applyInternal() = transactional() {
		department.removeOwner(usercode)
	}

	override def describe(d: Description) = d.properties(
		"department" -> department.code,
		"usercode" -> usercode)
}
