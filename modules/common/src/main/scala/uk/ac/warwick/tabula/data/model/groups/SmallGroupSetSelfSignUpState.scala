package uk.ac.warwick.tabula.data.model.groups

sealed trait SmallGroupSetSelfSignUpState {
	val name: String
}

object SmallGroupSetSelfSignUpState {
	case object Open extends SmallGroupSetSelfSignUpState {
		val name = "open"
	}
	case object Closed extends SmallGroupSetSelfSignUpState {
		val name = "close"
	}
	//Traditional comment - lame manual set keep in synch with above
	val members = Seq(Open, Closed)
	private[groups] def find(i: String) = members find { _.name == i }

	def apply(i: String): SmallGroupSetSelfSignUpState = find(i) getOrElse { throw new IllegalArgumentException(s"Invalid value for self sign-up state: $i") }

}


