package uk.ac.warwick.tabula.services.scheduling.jobs

import org.quartz.JobExecutionContext
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean

class RemovePersonalDataAfterRelationshipFinishedJob extends AutowiredJobBean {
	override def executeInternal(context: JobExecutionContext): Unit = {

		???
	}
}
