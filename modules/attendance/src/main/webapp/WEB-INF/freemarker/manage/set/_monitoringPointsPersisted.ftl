<#include "*/attendance_variables.ftl" />
<div class="monitoring-points">
	<#macro pointsInATerm term>
		<div class="striped-section">
			<h2 class="section-title">${term}</h2>
			<div class="striped-section-contents">
				<#list command.monitoringPointsByTerm[term]?sort_by("week") as point>
					<div class="item-info row-fluid point">
						<div class="span12">
							<div class="pull-right">
								<#local noUpdate = !command.canPointBeUpdated(point)/>
								<#local noRemove = !command.canPointBeRemoved(point)/>
								<a class="btn btn-primary edit-point <#if noUpdate>disabled</#if>" <#if noUpdate>title="Unavailable"</#if> href="<@url page="/manage/${point.pointSet.route.department.code}/sets/${point.pointSet.id}/points/${point.id}/edit"/>">
									Edit
								</a>
								<a class="btn btn-danger delete-point <#if noRemove>disabled</#if>" title="Delete<#if noRemove> (unavailable)</#if>" href="<@url page="/manage/${point.pointSet.route.department.code}/sets/${point.pointSet.id}/points/${point.id}/delete"/>">
									<i class="icon-remove"></i>
								</a>
							</div>
							${point.name} (<@fmt.weekRanges point />)
						</div>
					</div>
				</#list>
			</div>
		</div>
	</#macro>

	<#if command.set.points?size == 0>
		<div class="row-fluid">
			<div class="span12">
				<em>No points exist for this scheme</em>
			</div>
		</div>
	<#else>
		<#list monitoringPointTermNames as term>
			<#if command.monitoringPointsByTerm[term]??>
				<@pointsInATerm term/>
			</#if>
		</#list>
	</#if>
</div>