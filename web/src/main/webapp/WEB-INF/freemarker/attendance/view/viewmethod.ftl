<#escape x as x?html>

  <#function route_function dept>
    <#local result><@routes.attendance.viewHomeForYear dept academicYear /></#local>
    <#return result />
  </#function>
  <@fmt.id7_deptheader title="View and record attendance for ${academicYear.toString}" route_function=route_function preposition="in" />

  <#if hasSchemes>
    <ul>
      <li><h3><a href="<@routes.attendance.viewStudents department academicYear />">View by student<#if features.attendanceMonitoringReport && can.do("MonitoringPoints.Report", department)><#if features.attendanceMonitoringRealTimeReport></a>
          <#assign introText>
            <p>When a monitoring point is recorded as ‘Missed (unauthorised)’, this is immediately uploaded to SITS.
              Only unauthorised missed points are uploaded; authorised missed points are not.</p>

            <p>If the attendance/fulfilment of the point is subsequently changed to anything other than ‘Missed (unauthorised)’,
              the missed point is removed from SITS.</p>
          </#assign>
          <a href="#"
             id="attendance-monitoring-realtime"
             class="use-introductory<#if showIntro("attendance-monitoring-realtime", "anywhere")> auto</#if>"
             data-hash="${introHash("attendance-monitoring-realtime", "anywhere")}"
             data-title="Automatic upload of monitoring points to SITS"
             data-placement="right"
             data-html="true"
             aria-label="Help"
             data-content="${introText}"><i class="fa fa-question-circle"></i></a>
          <#else> and report to SITS</a></#if></#if></h3></li>
      <li><h3><a href="<@routes.attendance.viewPoints department academicYear />">View by point</a></h3></li>
      <#if can.do("MonitoringPoints.View", department)>
        <#list department.displayedStudentRelationshipTypes as relationshipType>
          <li><h3><a href="<@routes.attendance.viewAgents department academicYear relationshipType />">View by ${relationshipType.agentRole}</a></h3></li>
        </#list>
      </#if>
    </ul>
  <#else>
    <p class="alert alert-info">There are no monitoring point schemes for this department for ${academicYear.toString}.</p>
  </#if>

</#escape>
