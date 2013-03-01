package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import org.hibernate.criterion.Restrictions

trait MarkingWorkflowDao {
	
	/** All assignments using this marking workflow. */
	def getAssignmentsUsingMarkingWorkflow(markingWorkflow: MarkingWorkflow): Seq[Assignment]

}

@Repository
class MarkingWorkflowDaoImpl extends MarkingWorkflowDao with Daoisms {
	
	def getAssignmentsUsingMarkingWorkflow(markingWorkflow: MarkingWorkflow): Seq[Assignment] =
		session.newCriteria[Assignment]
			.add(Restrictions.eq("markingWorkflow", markingWorkflow))
			.seq

}