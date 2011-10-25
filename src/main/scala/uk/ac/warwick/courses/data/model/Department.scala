package uk.ac.warwick.courses.data.model
import scala.collection.JavaConversions.seqAsJavaList
import scala.reflect.BeanProperty
import org.hibernate.annotations.AccessType
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.PostLoad
import uk.ac.warwick.courses.data.PostLoadBehaviour
import javax.persistence.CascadeType

@Entity @AccessType("field")
class Department extends GeneratedId with PostLoadBehaviour {
  
	@BeanProperty var code:String = null
	
	@BeanProperty var name:String = null
	
	@OneToMany(mappedBy="department")
	@BeanProperty var modules:java.util.List[Module] = List()
	
	@OneToOne(cascade=Array(CascadeType.ALL))
	@JoinColumn(name="ownersgroup_id")
	@BeanProperty var owners:UserGroup = new UserGroup
	
	def isOwnedBy(userId:String) = owners.includes(userId)
	
	def addOwner(owner:String) = ensureOwners.addUser(owner)
	def removeOwner(owner:String) = ensureOwners.removeUser(owner)
	
	// If hibernate sets owners to null, make a new empty usergroup
	override def postLoad { 
	  ensureOwners
	}
	
	def ensureOwners = {
	  if (owners == null) owners = new UserGroup
	  owners
	}
	
	override def toString = "Department("+code+")"
	
}