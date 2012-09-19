package uk.ac.warwick.courses.commands

/**
 * Command that does nothing and returns nothing,
 * but you can configure it to do something for testing if you want.
 */
class NullCommand extends Command[Unit] {

	private var fn = () => {}
	def apply() = {
		fn()
	}
	def will(f: () => Unit) = {
		fn = f
		this
	}

	private var dfn = (d: Description) => {}
	def describe(d: Description) {
		dfn(d)
	}
	def describedAs(f: (Description) => Unit) = {
		dfn = f
		this
	}

}
