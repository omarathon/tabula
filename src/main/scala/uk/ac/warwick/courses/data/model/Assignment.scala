package uk.ac.warwick.courses.data.model
import scala.reflect._
import javax.persistence.Id
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.AccessType
import javax.persistence.OneToMany
import org.joda.time.DateTime
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.CascadeType
import org.hibernate.annotations.Type

@Entity @AccessType("field")
class Assignment extends GeneratedId {
  
	@BeanProperty var name:String =_
	@BeanProperty var active:Boolean =_
	
	
	
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var openDate:DateTime =_
	
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var closeDate:DateTime =_
	
	@ManyToOne(cascade=Array(CascadeType.PERSIST,CascadeType.MERGE))
	@JoinColumn(name="module_id")
	@BeanProperty var module:Module =_
	
	@OneToMany(mappedBy="assignment")
	@BeanProperty var attachments:java.util.Set[FileAttachment] =_
}