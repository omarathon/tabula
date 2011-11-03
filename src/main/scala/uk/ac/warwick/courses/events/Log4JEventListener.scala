package uk.ac.warwick.courses.events

import org.apache.log4j.Logger
import uk.ac.warwick.courses.commands.Describable
import uk.ac.warwick.courses.commands.DescriptionImpl
import uk.ac.warwick.courses.RequestInfo

class Log4JEventListener extends EventListener {
	
	val logger = Logger.getLogger("uk.ac.warwick.courses.AUDIT")
	
	override def beforeCommand(command: Describable) {
		val s = generateMessage(command, "pre-event")
		logger.info(s.toString)
	}

	override def afterCommand(command: Describable, returnValue: Any) {
		val s = generateMessage(command)
		logger.info(s.toString)
	}

	override def onException(command: Describable, exception: Throwable) {
		val s = generateMessage(command, "failed-event")
		logger.info(s.toString)
	}
	
	def generateMessage(command:Describable, eventStage:String="event") = {
		val s = new StringBuilder
		s ++= eventStage ++ "=" ++ command.eventName
		RequestInfo.fromThread match {
			case Some(info) => {
				val user = info.user
				s ++= " user=" ++ user.apparentId
				if (user.masquerading) {
					s ++= " realUser=" ++ user.realId
				}
			}
			case None => /* no requestinfo, ok if it's a scheduled job? */
		}
		describe(command, s)
		s
	}
	
	def describe(command:Describable, s:StringBuilder) = {
		val description = new DescriptionImpl
		command.describe(description)
		for ((key,value) <- description.allProperties)
			s ++= " " ++ key ++ "=" ++ value.toString
	}

}