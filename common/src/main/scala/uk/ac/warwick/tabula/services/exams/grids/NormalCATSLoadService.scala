package uk.ac.warwick.tabula.services.exams.grids

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntityYear
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model.{DegreeType, NormalCATSLoad, Route}
import uk.ac.warwick.tabula.data.{AutowiringNormalCATSLoadDaoComponent, NormalCATSLoadDaoComponent}

import scala.collection.mutable

case class NormalLoadLookup(yearOfStudy: YearOfStudy, normalCATSLoadService: NormalCATSLoadService) {
  private val cache = mutable.Map[(Route, AcademicYear), Option[BigDecimal]]()

  def withoutDefault(route: Route, academicYear: AcademicYear): Option[BigDecimal] = cache.get((route, academicYear)) match {
    case Some(option) =>
      option
    case _ =>
      cache.put((route, academicYear), normalCATSLoadService.find(route, academicYear, yearOfStudy).map(_.normalLoad))
      cache((route, academicYear))
  }

  def apply(route: Route, academicYear: AcademicYear): BigDecimal =
    Option(route).flatMap(withoutDefault(_, academicYear)).getOrElse(Option(route).map(_.degreeType).getOrElse(DegreeType.Undergraduate).normalCATSLoad)

  def apply(year: ExamGridEntityYear): BigDecimal = apply(year.route, year.baseAcademicYear)

  def routes: Seq[Route] = cache.keys.map(_._1).toSeq.distinct
}

trait NormalCATSLoadService {

  def saveOrUpdate(load: NormalCATSLoad): Unit

  def delete(load: NormalCATSLoad): Unit

  def find(route: Route, academicYear: AcademicYear, yearOfStudy: YearOfStudy): Option[NormalCATSLoad]

  def findAll(routes: Seq[Route], academicYear: AcademicYear): Seq[NormalCATSLoad]

}

abstract class AbstractNormalCATSLoadService extends NormalCATSLoadService {

  self: NormalCATSLoadDaoComponent =>

  def saveOrUpdate(load: NormalCATSLoad): Unit =
    normalCATSLoadDao.saveOrUpdate(load)

  def delete(load: NormalCATSLoad): Unit =
    normalCATSLoadDao.delete(load)

  def find(route: Route, academicYear: AcademicYear, yearOfStudy: YearOfStudy): Option[NormalCATSLoad] =
    normalCATSLoadDao.find(route, academicYear, yearOfStudy)

  def findAll(routes: Seq[Route], academicYear: AcademicYear): Seq[NormalCATSLoad] =
    normalCATSLoadDao.findAll(routes, academicYear)

}

@Service("normalCATSLoadService")
class NormalCATSLoadServiceImpl
  extends AbstractNormalCATSLoadService
    with AutowiringNormalCATSLoadDaoComponent

trait NormalCATSLoadServiceComponent {
  def normalCATSLoadService: NormalCATSLoadService
}

trait AutowiringNormalCATSLoadServiceComponent extends NormalCATSLoadServiceComponent {
  var normalCATSLoadService: NormalCATSLoadService = Wire[NormalCATSLoadService]
}
