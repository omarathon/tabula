<#escape x as x?html>
  <h1>Export profiles</h1>
  <h2>Filter student details</h2>

  <#assign submitUrl><@routes.reports.profileExport department academicYear /></#assign>
  <#assign filterCommand = command />
  <#assign filterCommandName = "command" />
  <#assign filterResultsPath = "/WEB-INF/freemarker/reports/profiles/_filter.ftl" />
  <#assign filterFormAddOn = "/WEB-INF/freemarker/reports/profiles/_filter-form-addon.ftl" />

  <#include "/WEB-INF/freemarker/filter_bar.ftl" />
</#escape>
