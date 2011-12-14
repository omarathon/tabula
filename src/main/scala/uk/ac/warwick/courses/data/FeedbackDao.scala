package uk.ac.warwick.courses.data
import uk.ac.warwick.courses.data.model.Feedback
import org.springframework.stereotype.Repository

trait FeedbackDao {
	def getFeedback(id:String): Option[Feedback]
}

@Repository
class FeedbackDaoImpl extends FeedbackDao with Daoisms {
	private val clazz = classOf[Feedback].getName
	
	override def getFeedback(id:String) = option[Feedback](session.get(clazz, id))
}