<#include "form.ftl" />
<#escape x as x?html>
  <#macro row result>
    <#local studentCourseYearDetails = result._1() />
    <#local benchmark = result._2() />

    <tr>
      <td>${studentCourseYearDetails.studentCourseDetails.sprCode}</td>
      <td>${studentCourseYearDetails.studentCourseDetails.student.firstName}</td>
      <td>${studentCourseYearDetails.studentCourseDetails.student.lastName}</td>
      <td>${studentCourseYearDetails.enrolmentDepartment.fullName}</td>
      <td>
        <a href="<@routes.exams.benchmarkDetails studentCourseYearDetails />">
          <#if benchmark.isRight()>
            ${benchmark.right().get()}
          <#else>
            ${benchmark.left().get()}
          </#if>
        </a>
      </td>
    </tr>
  </#macro>

  <table class="table table-striped">
    <thead>
      <tr>
        <th>SPR Code</th>
        <th>First name</th>
        <th>Last name</th>
        <th>Enrolment department</th>
        <th>Graduation benchmark</th>
      </tr>
    </thead>
    <tbody>
      <#list results as result>
        <@row result />
      </#list>
    </tbody>
  </table>
</#escape>
