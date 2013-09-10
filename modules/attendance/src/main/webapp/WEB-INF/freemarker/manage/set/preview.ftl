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
		<#list command.monitoringPointsByTerm[term]?sort_by("week") as point>
			<div class="item-info row-fluid point">
				<div class="span12">
					${point.name} (Week ${point.week}) <#-- <@fmt.singleWeekFormat point.week command.academicYearToUse command.dept />) -->
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