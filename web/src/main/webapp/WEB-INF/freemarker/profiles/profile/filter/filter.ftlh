<div class="deptheader">
  <h1>View students for ${academicYear.toString}</h1>
  <h4 class="with-related"><span class="muted">in</span> ${department.name}</h4>
</div>

<#assign filterCommand = filterStudentsCommand />
<#assign filterCommandName = "filterStudentsCommand" />
<#assign filterResultsPath = "/WEB-INF/freemarker/profiles/profile/filter/results.ftlh" />
<#assign url><@routes.profiles.filter_students department academicYear/></#assign>
<#assign submitUrl= url?markup_string />
<!--filter_bar is invoked from other ftl files so once we convert all ftl to ftls can use submitUrl with markup_string directly there -->
<#include "/WEB-INF/freemarker/filter_bar.ftl" />
