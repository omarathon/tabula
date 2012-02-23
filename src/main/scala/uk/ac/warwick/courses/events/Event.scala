package uk.ac.warwick.courses.events
import org.joda.time.DateTime
import uk.ac.warwick.courses.commands.DescriptionImpl
import uk.ac.warwick.courses.commands.Describable
import uk.ac.warwick.courses.RequestInfo
import uk.ac.warwick.courses.commands.Description
import java.util.UUID

case class Event(
	val id:String,
    val name:String,
    val userId:String,
    val realUserId:String,
    val extra:Map[String,Any],
	val date:DateTime=new DateTime
)

object Event {
	def fromDescribable(describable:Describable) = doFromDescribable(describable, false)
	def resultFromDescribable(describable:Describable, id:String) = doFromDescribable(describable, true, id)
	
	private def doFromDescribable(describable:Describable, result:Boolean, id:String = null) = {
		val description = new DescriptionImpl
		if (result) { 
			describable.describeResult(description)
		} else { 
			describable.describe(description) 
		}
		val (apparentId, realUserId) = getUser match {
			case Some(user) => (user.apparentId, user.realId)
			case None => (null, null)
		}
		val eventId = id match {
			case id:String => id
			case _ => UUID.randomUUID.toString
		}
		new Event(
			eventId,
			describable.eventName,
			apparentId,
			realUserId,
			description.allProperties.toMap
		)
	}
	
	private def getUser = RequestInfo.fromThread match {
		case Some(info) => Some(info.user)
		case None => None
	}
}