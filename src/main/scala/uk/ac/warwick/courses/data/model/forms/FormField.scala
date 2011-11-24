package uk.ac.warwick.courses.data.model.forms
import org.hibernate.annotations.AccessType
import javax.persistence.Entity
import javax.persistence.Inheritance
import javax.persistence.InheritanceType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.GeneratedId
import javax.persistence.Column
import javax.persistence.DiscriminatorColumn
import javax.persistence.DiscriminatorValue
import org.springframework.validation.Validator
import org.springframework.validation.Errors

@Entity @AccessType("field")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="fieldtype")
abstract class FormField extends GeneratedId /*with Validator*/ {
	
	@ManyToOne
	@JoinColumn(name="assignment_id")
	var assignment:Assignment =_
	
	var name:String =_
	var label:String =_
	var instructions:String =_
	var required:Boolean =_
	
	@Column(name="properties")
	var propertiesString:String =_
	
	// list position
	var position:Int =_
	
//	def supports(cls:Class[_]) = class
//	def validate(self:Any, errors:Errors) {
//		
//	}
}

@DiscriminatorValue("text")
class TextField extends FormField {
	
}

@DiscriminatorValue("textarea")
class TextareaField extends FormField {
	
}

@DiscriminatorValue("checkbox")
class CheckboxField extends FormField {
	
}

@DiscriminatorValue("select")
class SelectField extends FormField {
	
}

