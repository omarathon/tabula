package uk.ac.warwick.tabula.data

import org.hibernate.criterion.Order._
import org.hibernate.criterion.Restrictions._
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Department, DepartmentMarkingDescriptor, MarkingDescriptor, UniversityMarkingDescriptor}

trait MarkingDescriptorDaoComponent {
	val markingDescriptorDao: MarkingDescriptorDao
}

trait AutowiringMarkingDescriptorDaoComponent extends MarkingDescriptorDaoComponent {
	override val markingDescriptorDao: MarkingDescriptorDao = Wire[MarkingDescriptorDao]
}

trait MarkingDescriptorDao {
	def get(id: String): Option[MarkingDescriptor]

	def getMarkingDescriptors: Seq[UniversityMarkingDescriptor]

	def getDepartmentMarkingDescriptors(department: Department): Seq[DepartmentMarkingDescriptor]

	def getForMark(mark: Int, department: Department): MarkingDescriptor

	def save(markingDescriptor: MarkingDescriptor): Unit

	def delete(markingDescriptor: MarkingDescriptor): Unit
}

@Repository
class MarkingDescriptorDaoImpl extends MarkingDescriptorDao with Daoisms {
	override def getMarkingDescriptors: Seq[UniversityMarkingDescriptor] = {
		session.newCriteria[UniversityMarkingDescriptor]
			.addOrder(asc("minMark"))
			.seq
	}

	override def getDepartmentMarkingDescriptors(department: Department): Seq[DepartmentMarkingDescriptor] = {
		session.newCriteria[DepartmentMarkingDescriptor]
			.add(is("department", department))
			.addOrder(asc("minMark"))
			.seq
	}

	override def getForMark(mark: Int, department: Department): MarkingDescriptor = {
		val departmentMarkingDescriptor = session.newCriteria[DepartmentMarkingDescriptor]
			.add(is("department", department))
			.add(ge("minMark", mark))
			.add(le("maxMark", mark))
			.setMaxResults(1)
			.uniqueResult

		departmentMarkingDescriptor.orElse {
			session.newCriteria[MarkingDescriptor]
				.add(ge("minMark", mark))
				.add(le("maxMark", mark))
				.setMaxResults(1)
				.uniqueResult
		}.getOrElse(throw new IllegalArgumentException(s"No marking descriptor for mark of $mark"))
	}

	override def save(markingDescriptor: MarkingDescriptor): Unit = session.saveOrUpdate(markingDescriptor)

	override def delete(markingDescriptor: MarkingDescriptor): Unit = session.delete(markingDescriptor)

	override def get(id: String): Option[MarkingDescriptor] = getById[MarkingDescriptor](id)
}
