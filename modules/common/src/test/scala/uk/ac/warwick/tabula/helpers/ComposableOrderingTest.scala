package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.{Fixtures, TestBase}

class ComposableOrderingTest extends TestBase {

	val student1 = Fixtures.student("1234")
	student1.firstName = "Dave"
	student1.lastName = "Lister"

	val student2 = Fixtures.student("2345")
	student2.firstName = "Arnold"
	student2.lastName = "Rimmer"

	val student3 = Fixtures.student("3456")
	student3.firstName = "Ace"
	student3.lastName = "Rimmer"

	val student4 = Fixtures.student("3457")
	student4.firstName = "Ace"
	student4.lastName = "Rimmer"

	val students = Seq(student1, student2, student3)

	@Test
	def singleOrdering(): Unit = {
		val c = new ComposableOrdering[StudentMember](Ordering.by[StudentMember, String](_.firstName))
		val sorted = students.sorted(c)
		sorted.head should be (student3)
	}

	@Test
	def singleOrderingReverse(): Unit = {
		val c = new ComposableOrdering[StudentMember](Ordering.by[StudentMember, String](_.firstName)(Ordering.String.reverse))
		val sorted = students.sorted(c)
		sorted.head should be (student1)
	}

	@Test
	def doubleOrdering(): Unit = {
		val o1 = Ordering.by[StudentMember, String](_.lastName) // student2.lastName == student3.lastName
		val o2 = Ordering.by[StudentMember, String](_.firstName) // student3.firstName < student2.firstName
		val c = new ComposableOrdering[StudentMember](o1).andThen(o2)
		val sorted = students.sorted(c)
		sorted should be (Seq(student1, student3, student2))
	}

	@Test
	def doubleOrderingWithReverse(): Unit = {
		val o1 = Ordering.by[StudentMember, String](_.lastName) // student2.lastName == student3.lastName
		val o2 = Ordering.by[StudentMember, String](_.firstName)(Ordering.String.reverse) // student3.firstName < student2.firstName
		val c = new ComposableOrdering[StudentMember](o1).andThen(o2)
		val sorted = students.sorted(c)
		sorted should be (Seq(student1, student2, student3))
	}

	@Test
	def tripleOrdering(): Unit = {
		val o1 = Ordering.by[StudentMember, String](_.lastName) // student3.lastName == student4.lastName
		val o2 = Ordering.by[StudentMember, String](_.firstName) // student3.firstName == student4.firstName
		val o3 = Ordering.by[StudentMember, String](_.universityId) // student3.universityId < student4.universityId
		val c = new ComposableOrdering[StudentMember](o1).andThen(o2).andThen(o3)
		val sorted = (students ++ Seq(student4)).sorted(c)
		sorted should be (Seq(student1, student3, student4, student2))
	}

	@Test
	def tripleOrderingReverse(): Unit = {
		val o1 = Ordering.by[StudentMember, String](_.lastName)(Ordering.String.reverse) // student3.lastName == student4.lastName
		val o2 = Ordering.by[StudentMember, String](_.firstName) // student3.firstName == student4.firstName
		val o3 = Ordering.by[StudentMember, String](_.universityId)(Ordering.String.reverse) // student3.universityId > student4.universityId
		val c = new ComposableOrdering[StudentMember](o1).andThen(o2).andThen(o3)
		val sorted = (students ++ Seq(student4)).sorted(c)
		sorted should be (Seq(student4, student3, student2, student1))
	}

}
