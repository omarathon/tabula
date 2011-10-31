package uk.ac.warwick.courses.audit
import org.joda.time.DateTime

class Event(
    val name:String,
    val user:String,
    val realUser:String,
    val item:String,
    val extra:Map[String,String],
	val date:DateTime=new DateTime
)