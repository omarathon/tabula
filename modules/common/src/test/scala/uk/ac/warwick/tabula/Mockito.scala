package uk.ac.warwick.tabula

import org.specs2.specification.AllExpectations
import scala.reflect.ClassTag

trait Mockito extends org.specs2.mock.Mockito with AllExpectations {
  def isEq[A](arg:A) = argThat(org.hamcrest.Matchers.equalTo(arg))
  def isNotEq[A](arg:A) = argThat(org.hamcrest.Matchers.not(org.hamcrest.Matchers.equalTo(arg)))
	def isA[A](implicit tag: ClassTag[A]) = org.mockito.Matchers.isA(tag.runtimeClass.asInstanceOf[Class[A]])
	def isNull[A](implicit tag: ClassTag[A]) = org.mockito.Matchers.isNull(tag.runtimeClass.asInstanceOf[Class[A]])
	def reset[A](arg:A) = org.mockito.Mockito.reset(arg)

	def verifyNoMoreInteractions(mocks: AnyRef*) = org.mockito.Mockito.verifyNoMoreInteractions(mocks : _*)
}