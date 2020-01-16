<#escape x as x?html>
  <#import "../attendance_macros.ftl" as attendance_macros />

  <#assign filterQuery = filterCommand.serializeFilter />

  <#if features.attendanceMonitoringReport && can.do("MonitoringPoints.Report", department)>
    <div class="pull-right send-to-sits">
      <#if features.attendanceMonitoringRealTimeReport>
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
           data-placement="left"
           data-html="true"
           aria-label="Help"
           data-content="${introText}"><i class="fa fa-question-circle"></i></a>
      <#else>
        <a href="<@routes.attendance.viewReport department academicYear filterQuery />" class="btn btn-primary">Upload to SITS</a>
      </#if>
    </div>
  </#if>

  <#function route_function dept>
    <#local result><@routes.attendance.viewStudents dept academicYear /></#local>
    <#return result />
  </#function>
  <@fmt.id7_deptheader title="View students" route_function=route_function preposition="in" />

  <#if reports?? && monitoringPeriod??>
    <div class="alert alert-info">
      <button type="button" class="close" data-dismiss="alert">&times;</button>
      Missed points for <@fmt.p reports "student" /> in the ${monitoringPeriod} monitoring period have been uploaded to SITS.
    </div>
  </#if>

  <#assign submitUrl><@routes.attendance.viewStudents department academicYear /></#assign>
  <#assign filterCommand = filterCommand />
  <#assign filterCommandName = "filterCommand" />
  <#assign filterResultsPath = "/WEB-INF/freemarker/attendance/view/_students_results.ftl" />

  <#include "/WEB-INF/freemarker/filter_bar.ftl" />

  <script type="text/javascript" nonce="${nonce()}">
    jQuery(window).on('load', function () {
      GlobalScripts.scrollableTableSetup();
    });
    jQuery(function ($) {
      $('#command').find('input').on('change', function (e) {
        $('.send-to-sits a').addClass('disabled');
      });

      $(document).on("tabula.filterResultsChanged", function () {
        var sitsUrl = $('div.studentResults').data('sits-url');
        $('.send-to-sits a').attr('href', sitsUrl).removeClass('disabled');
      });
    });
  </script>
</#escape>
