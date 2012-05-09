package uk.ac.warwick.courses.data.model

import javax.persistence.Entity

/**
 * Represents an upstream assignment as found in the central
 * University systems. An upstream assignment is timeless - it doesn't
 * relate to a specific instance of an assignment or even a particular year.
 */
@Entity
class UpstreamAssignment extends GeneratedId {
	/**
	 * Lowercase module code, with CATS. e.g. in304-15
	 */
	var moduleCode:String =_
	/**
	 * Assessment group the assignment is in. Is mostly a meaningless
	 * character but will map to a corresponding student module registratation
	 * to the same group.
	 */
	var assessmentGroup:String =_
	/**
	 * Identifier for the assignment, unique within a given moduleCode.
	 */
	var sequence:String =_
	/**
	 * Lowercase department code, e.g. md.
	 */
	var departmentCode:String =_
	/**
	 * Name as defined upstream.
	 */
	var name:String =_
	
	/**
	 * Returns moduleCode without CATS. e.g. in304
	 */
	def moduleCodeBasic = Module.stripCats(moduleCode)
}

