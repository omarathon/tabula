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

@Entity @AccessType("field")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="fieldtype")
abstract class FormField extends GeneratedId {
	
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
	
}

@DiscriminatorValue("text")
class TextField extends FormField {
	
}

@DiscriminatorValue("file")
class FileField extends FormField {
	
}