package uk.ac.warwick.tabula.exams.grids

import uk.ac.warwick.tabula.jobs.Job
import uk.ac.warwick.tabula.services.jobs.JobInstance

trait StatusAdapter {
	def setMessage(message: String): Unit

	def setProgress(percentage: Int): Unit

	def setProgress(current: Int, max: Int): Unit = setProgress(((current.toFloat / max.toFloat) * 100).toInt)
}

object NullStatusAdapter extends StatusAdapter {
	override def setMessage(message: String): Unit = ()

	override def setProgress(percentage: Int): Unit = ()
}

class JobInstanceStatusAdapter(job: Job, instance: JobInstance) extends StatusAdapter {
	override def setMessage(message: String): Unit = job.updateStatus(message)(instance)

	override def setProgress(percentage: Int): Unit = job.updateProgress(percentage)(instance)
}
