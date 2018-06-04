package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.AutowiringMarkingDescriptorDaoComponent
import uk.ac.warwick.tabula.data.model._

trait MarkingDescriptorServiceComponent {
	val markingDescriptorService: MarkingDescriptorService
}

trait AutowiringMarkingDescriptorServiceComponent extends MarkingDescriptorServiceComponent {
	override val markingDescriptorService: MarkingDescriptorService = Wire[MarkingDescriptorService]
}

trait MarkingDescriptorService {
	def get(id: String): Option[MarkingDescriptor]

	def getMarkingDescriptors: Seq[UniversityMarkingDescriptor]

	def getDepartmentMarkingDescriptors(department: Department): Seq[DepartmentMarkingDescriptor]

	def getForMark(mark: Int, department: Department): MarkingDescriptor

	def save(markingDescriptor: MarkingDescriptor)

	def delete(markingDescriptor: MarkingDescriptor)
}

@Service("markingDescriptorService")
class MarkingDescriptorServiceImpl extends MarkingDescriptorService with AutowiringMarkingDescriptorDaoComponent {
	private def dao = markingDescriptorDao

	override def getMarkingDescriptors: Seq[UniversityMarkingDescriptor] = dao.getMarkingDescriptors

	override def getDepartmentMarkingDescriptors(department: Department): Seq[DepartmentMarkingDescriptor] = dao.getDepartmentMarkingDescriptors(department)

	override def getForMark(mark: Int, department: Department): MarkingDescriptor = dao.getForMark(mark, department)

	override def save(markingDescriptor: MarkingDescriptor): Unit = dao.save(markingDescriptor)

	override def delete(markingDescriptor: MarkingDescriptor): Unit = dao.delete(markingDescriptor)

	override def get(id: String): Option[MarkingDescriptor] = dao.get(id)
}
