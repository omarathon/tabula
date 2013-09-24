<#-- Template for displaying a read-only view of a set inside -->
<#include "*/attendance_variables.ftl" />
<#import "*/modal_macros.ftl" as modal />
<#assign set = command.set />

<@modal.header>
	<h2>Preview ${set.templateName!'point set'}</h2>
</@modal.header>

<#macro pointsInATerm term>
<div class="striped-section">
	<h2 class="section-title">${term}</h2>
	<div class="striped-section-contents">
		<#list command.monitoringPointsByTerm[term]?sort_by("validFromWeek") as point>
			<div class="item-info row-fluid point">
				<div class="span12">
					${point.name} (<@fmt.singleWeekFormat point.validFromWeek point.requiredFromWeek command.academicYearToUse command.department />)
				</div>
			</div>
		</#list>
	</div>
</div>
</#macro>

<@modal.body>
<#list monitoringPointTermNames as term>
	<#if command.monitoringPointsByTerm[term]??>
		<@pointsInATerm term/>
	</#if>
</#list>
</@modal.body>