<#import '*/reports_macros.ftl' as reports_macros />

<h1>Recorded late event attendance registers</h1>

<#assign reportUrl><@routes.reports.recordedLateSmallGroupAttendanceRegisters department academicYear /></#assign>
<@reports_macros.reportLoader reportUrl>
  <ul class="dropdown-menu">
    <li><a href="#" data-href="<@routes.reports.recordedLateSmallGroupAttendanceRegistersDownloadCsv department academicYear />">CSV</a></li>
    <li><a href="#" data-href="<@routes.reports.recordedLateSmallGroupAttendanceRegistersDownloadXlsx department academicYear />">Excel</a></li>
    <li><a href="#" data-href="<@routes.reports.recordedLateSmallGroupAttendanceRegistersDownloadXml department academicYear />">XML</a></li>
  </ul>
</@reports_macros.reportLoader>
<@reports_macros.smallGroupAttendanceRegisterReportScript />
