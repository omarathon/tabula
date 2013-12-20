package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import org.hibernate.criterion.Restrictions
import uk.ac.warwick.spring.Wire

trait MarkingWorkflowDaoComponent {
	def markingWorkflowDao: MarkingWorkflowDao
}

trait AutowiringMarkingWorkflowDaoComponent extends MarkingWorkflowDaoComponent {
	var markingWorkflowDao = Wire[MarkingWorkflowDao]
}

trait MarkingWorkflowDao {
	def save(markingWorkflow: MarkingWorkflow): Unit
	
	/** All assignments using this marking workflow. */
	def getAssignmentsUsingMarkingWorkflow(markingWorkflow: MarkingWorkflow): Seq[Assignment]

}

@Repository
class MarkingWorkflowDaoImpl extends MarkingWorkflowDao with Daoisms {
	
	def save(markingWorkflow: MarkingWorkflow) = session.saveOrUpdate(markingWorkflow)
	
	def getAssignmentsUsingMarkingWorkflow(markingWorkflow: MarkingWorkflow): Seq[Assignment] =
		session.newCriteria[Assignment]
			.add(is("markingWorkflow", markingWorkflow))
			.seq

}