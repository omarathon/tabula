package uk.ac.warwick.courses.events
import org.joda.time.DateTime

case class Event(
    val name:String,
    val userId:String,
    val realUserId:String,
    val extra:Map[String,Any],
	val date:DateTime=new DateTime
)