package uk.ac.warwick.courses.jobs

/**
 * Initial information generated to give to the JobService to
 * create a JobInstance that then gets saved. Usually created
 * by a factory method in a particular Job.
 */
case class JobPrototype(val identifier: String, val map: Map[String, Any])