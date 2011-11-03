package uk.ac.warwick.courses.events
import org.joda.time.DateTime

case class Event(
    val name:String,
    val user:String,
    val realUser:String,
    val item:String,
    val extra:Map[String,String],
	val date:DateTime=new DateTime
)