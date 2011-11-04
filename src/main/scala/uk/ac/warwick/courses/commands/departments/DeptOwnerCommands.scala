package uk.ac.warwick.courses.commands.departments

import scala.reflect.BeanProperty
import uk.ac.warwick.courses.commands._
import uk.ac.warwick.courses.validators.UniqueUsercode
import uk.ac.warwick.courses.validators.ValidUsercode
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.data.model.Department
import org.hibernate.validator.constraints.NotEmpty
import org.springframework.transaction.annotation.Transactional

@UniqueUsercode(fieldName="usercode", collectionName="usercodes", message="usercode already in group")
class AddDeptOwnerCommand(val department:Department) extends Command[Unit] with Daoisms {
	
	def getUsercodes:Seq[String] = department.owners.members
	
	@NotEmpty
	@ValidUsercode(message="invalid usercode")
	@BeanProperty var usercode:String =_
	
	@Transactional
	override def apply = department.addOwner(usercode)
	
	override def describe(d:Description) = d.properties(
		"department" -> department.code,
		"usercode" -> usercode
	)
}
 
class RemoveDeptOwnerCommand(val department:Department) extends Command[Unit]  with Daoisms {
	
	def getUsercodes:Seq[String] = department.owners.members
	 
	@NotEmpty
	@ValidUsercode(message="invalid usercode")
	@BeanProperty var usercode:String =_
	
	@Transactional
	override def apply = department.removeOwner(usercode)
	
	override def describe(d:Description) = d.properties(
		"department" -> department.code,
		"usercode" -> usercode
	)
}
