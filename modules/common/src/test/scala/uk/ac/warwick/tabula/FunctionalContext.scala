package uk.ac.warwick.tabula

import org.springframework.scala.context.function.{FunctionalConfigApplicationContext, FunctionalConfiguration}
import uk.ac.warwick.spring.SpringConfigurer

abstract class FunctionalContext extends FunctionalConfiguration {
	bean[SpringConfigurer](name="springConfigurer") {
		new SpringConfigurer()
	}
}

trait FunctionalContextTesting {

	def inContext[A <: FunctionalContext : Manifest](fn: => Unit) {
		def use(ctx: FunctionalConfigApplicationContext, fn: => Unit) {
			try fn
			finally ctx.destroy()
		}
		use(FunctionalConfigApplicationContext[A](), fn)
	}

}
