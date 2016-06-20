<#include "*/attendance_variables.ftl" />
<div class="monitoring-points">
	<#macro pointsInATerm term>
		<div class="striped-section">
			<h2 class="section-title">${term}</h2>
			<div class="striped-section-contents">
				<#list command.monitoringPointsByTerm[term] as point>
					<div class="item-info row-fluid point">
						<div class="span12">
							<div class="pull-right">
								<#local noUpdate = !command.canPointBeUpdated(point)/>
								<#local noRemove = !command.canPointBeRemoved(point)/>
								<a class="btn btn-primary edit-point use-tooltip <#if noUpdate>disabled</#if>"
								   data-container="body"
								   <#if noUpdate>title="You cannot edit a monitoring point once it has attendance recorded against it"</#if>
								   href="<@routes.attendance.updatePoint point />"
								>
									Edit
								</a>
								<a class="btn btn-danger delete-point use-tooltip <#if noRemove>disabled</#if>"
								   data-container="body"
								   title="<#if noRemove>You cannot delete a monitoring point once it has attendance recorded against it<#else>Delete</#if>"
								   href="<@routes.attendance.removePoint point />"
								>
									<i class="icon-remove"></i>
								</a>
							</div>
							${point.name} (<a class="use-tooltip" data-html="true" title="<@fmt.wholeWeekDateFormat point.validFromWeek point.requiredFromWeek point.pointSet.academicYear />"><@fmt.monitoringPointWeeksFormat point.validFromWeek point.requiredFromWeek point.pointSet.academicYear point.pointSet.route.adminDepartment /></a>)
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
<script>
	jQuery(function($){
		$('.use-tooltip').tooltip();
	})
</script>