package uk.ac.warwick.courses.helpers

import java.lang.{Runnable => JRunnable}

/**
 * Wrapper for java.lang.Runnable that lets you pass in a function to
 * run, which is more concise.
 */
final class Runnable(f: =>Unit) extends JRunnable {
	override def run = f
}

/**
 * Easy Runnable.
 * val task = Runnable { println("Yeah!") }
 */
object Runnable {
	def apply(f: =>Unit) = new Runnable(f)
	
	// Import this if you want implicit conversion.
	implicit def ImplicitConversion(f: => Unit):JRunnable = Runnable(f)
}