package uk.ac.warwick.courses.web.forms
import uk.ac.warwick.courses.data.model.Department
import org.hibernate.validator.constraints.NotEmpty
import scala.reflect.BeanProperty
import uk.ac.warwick.courses.validators.ValidUsercode
import org.springframework.validation.Errors
import org.springframework.validation.beanvalidation.SpringValidatorAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.Validator
import uk.ac.warwick.courses.validators.UniqueUsercode

@UniqueUsercode(fieldName="usercode", collectionName="usercodes", message="usercode already in group")
class DepartmentAddOwnerForm(@BeanProperty val usercodes:Seq[String]) {
	@NotEmpty
	@ValidUsercode(message="invalid usercode")
	@BeanProperty var usercode:String =_
}

class DepartmentRemoveOwnerForm(@BeanProperty val usercodes:Seq[String]) {
	@NotEmpty
	@ValidUsercode(message="invalid usercode")
	@BeanProperty var usercode:String =_
}
