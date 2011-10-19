package uk.ac.warwick.courses.data.model
import org.hibernate.annotations.AccessType
import javax.persistence.Entity
import javax.persistence.Inheritance
import javax.persistence.InheritanceType
import javax.persistence.DiscriminatorColumn

//@Entity @AccessType("field")
class Permission {
	var user:String = _
	var level:String = _
}