package uk.ac.warwick.courses

import scala.util.Random

/**
 * Not actually used anywhere but we could use it as an auxiliary ID for something
 * that needed a short URL, like an assignment.
 */
object ShortCode {
	
	def random():String = Random.alphanumeric.take(5).mkString("")

}