package uk.ac.warwick.tabula.data

import org.hibernate.criterion.{Order, Restrictions}
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Assignment, OriginalityReport}

trait OriginalityReportDaoComponent {
	def originalityReportDao: OriginalityReportDao
}

trait AutowiringOriginalityReportDaoComponent extends OriginalityReportDaoComponent {
	val originalityReportDao: OriginalityReportDao = Wire[OriginalityReportDao]
}

trait OriginalityReportDao {
	def findReportToSubmit: Option[OriginalityReport]

	def findReportToRetrieve: Option[OriginalityReport]

	def listOriginalityReports(assignment: Assignment): Seq[OriginalityReport]
}

@Repository
class OriginalityReportDaoImpl extends OriginalityReportDao with Daoisms {
	private def criteria: ScalaCriteria[OriginalityReport] =
		session.newCriteria[OriginalityReport]
			.createAlias("attachment", "attachment") // Don't need these aliases
			.createAlias("attachment.submissionValue", "submissionValue") // but it ensures the submission
			.createAlias("submissionValue.submission", "submission") // hasn't been deleted

	override def findReportToSubmit: Option[OriginalityReport] = {
		criteria
			.add(Restrictions.lt("nextSubmitAttempt", DateTime.now))
			.add(Restrictions.isNull("nextResponseAttempt"))
			.addOrder(Order.asc("nextSubmitAttempt"))
			.setMaxResults(1)
			.uniqueResult
	}

	override def findReportToRetrieve: Option[OriginalityReport] = {
		criteria
			.add(Restrictions.isNull("nextSubmitAttempt"))
			.add(Restrictions.lt("nextResponseAttempt", DateTime.now))
			.addOrder(Order.asc("nextResponseAttempt"))
			.setMaxResults(1)
			.uniqueResult
	}

	override def listOriginalityReports(assignment: Assignment): Seq[OriginalityReport] = {
		criteria
			.add(is("submission.assignment", assignment))
			.seq
	}
}
