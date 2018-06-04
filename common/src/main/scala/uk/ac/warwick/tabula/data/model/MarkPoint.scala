package uk.ac.warwick.tabula.data.model

import java.util.Comparator

import scala.util.Try

sealed case class MarkPoint(mark: Int, markClass: MarkClass, name: String) extends Comparable[MarkPoint] {
	override def compareTo(o: MarkPoint): Int = MarkPoint.comparator.compare(this, o)

	def next: Option[MarkPoint] = Try(MarkPoint.all(MarkPoint.all.indexOf(this) + 1)).toOption

	def previous: Option[MarkPoint] = Try(MarkPoint.all(MarkPoint.all.indexOf(this) - 1)).toOption

	def min: Boolean = MarkPoint.all.startsWith(Seq(this))

	def max: Boolean = MarkPoint.all.endsWith(Seq(this))
}

sealed case class MarkClass(name: String)

object MarkClass {
	val Zero = MarkClass("Zero")
	val Fail = MarkClass("Fail")
	val Third = MarkClass("Third")
	val Second = MarkClass("Second")
	val First = MarkClass("First")

	val all: Seq[MarkClass] = Seq(
		Zero,
		Fail,
		Third,
		Second,
		First
	)
}

object MarkPoint {

	import MarkClass._

	val comparator: Comparator[MarkPoint] = Comparator.comparingInt(_.mark)

	def forMark(mark: Int): MarkPoint = all.find(_.mark == mark).getOrElse(throw new IllegalArgumentException(s"Invalid mark point $mark"))

	val all: Seq[MarkPoint] = Seq(
		MarkPoint(0, Zero, "Zero"),
		MarkPoint(12, Fail, "Low Fail"),
		MarkPoint(25, Fail, "Fail"),
		MarkPoint(32, Fail, "Fail"),
		MarkPoint(38, Fail, "High Fail (sub Honours)"),
		MarkPoint(42, Third, "Low 3rd"),
		MarkPoint(45, Third, "Mid 3rd"),
		MarkPoint(48, Third, "High 3rd"),
		MarkPoint(52, Second, "Low 2.2"),
		MarkPoint(55, Second, "Mid 2.2"),
		MarkPoint(58, Second, "High 2.2"),
		MarkPoint(62, Second, "Low 2.1"),
		MarkPoint(65, Second, "Mid 2.1"),
		MarkPoint(68, Second, "High 2.1"),
		MarkPoint(74, First, "Low 1st"),
		MarkPoint(78, First, "Lower Mid 1st"),
		MarkPoint(82, First, "Upper Mid 1st"),
		MarkPoint(88, First, "High 1st"),
		MarkPoint(94, First, "Excellent 1st"),
		MarkPoint(100, First, "Excellent 1st")
	)

	val allByClass: Seq[(MarkClass, Seq[MarkPoint])] = all.groupBy(_.markClass).toSeq
}
