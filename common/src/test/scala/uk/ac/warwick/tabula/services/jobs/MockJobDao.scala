package uk.ac.warwick.tabula.services.jobs

import java.util.UUID

import scala.collection.mutable.ArrayBuffer

class MockJobDao extends JobDao {

  val instances: ArrayBuffer[JobInstance] = ArrayBuffer[JobInstance]()

  def findOutstandingInstances(max: Int): Seq[JobInstance] = {
    instances.iterator.filterNot(_.started).take(max).toSeq
  }

  def findOutstandingInstance(example: JobInstance): Option[JobInstance] = {
    instances.filterNot(_.started).find { inst =>
      inst.jobType == example.jobType &&
        inst.json == example.json
    }
  }

  def saveJob(instance: JobInstance): JobInstance = {
    instance.id = UUID.randomUUID.toString
    instances += instance
    instance
  }

  def unfinishedInstances: Seq[JobInstance] = instances.toSeq.filterNot(_.finished)

  def listRecent(start: Int, count: Int): Seq[JobInstance] = instances.toSeq.filter(_.finished).slice(start, start + count)

  def getById(id: String): Option[JobInstance] = instances.find(id == _.id)

  def clear(): Unit = instances.clear

  def update(instance: JobInstance): Unit = {}

  override def listRunningJobs: Seq[JobInstance] = instances.toSeq.filterNot(_.finished).filter(_.started)

  override def findRunningJobs(schedulerInstance: String): Seq[JobInstance] = listRunningJobs.filter(_.schedulerInstance == schedulerInstance)
}
