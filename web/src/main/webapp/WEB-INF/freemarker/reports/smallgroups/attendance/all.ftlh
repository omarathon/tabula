<#import '*/reports_macros.ftl' as reports_macros />

<h1>All event attendance registers</h1>

<#assign reportUrl><@routes.reports.allSmallGroupAttendanceRegisters department academicYear /></#assign>
<@reports_macros.reportLoader reportUrl>
  <ul class="dropdown-menu">
    <li><a href="#" data-href="<@routes.reports.allSmallGroupAttendanceRegistersDownloadCsv department academicYear />">CSV</a></li>
    <li><a href="#" data-href="<@routes.reports.allSmallGroupAttendanceRegistersDownloadXlsx department academicYear />">Excel</a></li>
    <li><a href="#" data-href="<@routes.reports.allSmallGroupAttendanceRegistersDownloadXml department academicYear />">XML</a></li>
  </ul>
</@reports_macros.reportLoader>
<@reports_macros.smallGroupAttendanceRegisterReportScript />
