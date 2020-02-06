<#import '*/reports_macros.ftl' as reports_macros />

<h1>Unrecorded event attendance registers</h1>

<#assign reportUrl><@routes.reports.unrecordedSmallGroupAttendanceRegisters department academicYear /></#assign>
<@reports_macros.reportLoader reportUrl>
  <ul class="dropdown-menu">
    <li><a href="#" data-href="<@routes.reports.unrecordedSmallGroupAttendanceRegistersDownloadCsv department academicYear />">CSV</a></li>
    <li><a href="#" data-href="<@routes.reports.unrecordedSmallGroupAttendanceRegistersDownloadXlsx department academicYear />">Excel</a></li>
    <li><a href="#" data-href="<@routes.reports.unrecordedSmallGroupAttendanceRegistersDownloadXml department academicYear />">XML</a></li>
  </ul>
</@reports_macros.reportLoader>
<@reports_macros.smallGroupAttendanceRegisterReportScript />
