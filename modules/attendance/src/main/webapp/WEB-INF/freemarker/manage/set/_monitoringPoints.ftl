<#include "*/attendance_variables.ftl" />
<div class="monitoring-points">
	<@spring.bind path="command.monitoringPoints">
		<#if status.error>
			<div class="alert alert-error"><@f.errors path="command.monitoringPoints" cssClass="error"/></div>
		</#if>
	</@spring.bind>

	<#assign pointCount = 0 />
	<#assign pointFields = ["name", "validFromWeek", "requiredFromWeek", "pointType", "meetingRelationships", "meetingFormats", "meetingQuantity"] />
	<#macro pointsInATerm term>
		<div class="striped-section">
			<h2 class="section-title">${term}</h2>
			<div class="striped-section-contents">
				<#list command.monitoringPointsByTerm[term] as point>
					<div class="item-info row-fluid point">
						<div class="span12">
							<div class="pull-right">
								<a class="btn btn-primary edit-point" href="<@routes.editPoint command.dept pointCount />?form=true">Update</a>
								<a class="btn btn-danger delete-point" title="Delete" href="<@routes.deletePoint command.dept pointCount />?form=true"><i class="icon-remove"></i></a>
							</div>
							${point.name} (<@fmt.monitoringPointWeeksFormat point.validFromWeek point.requiredFromWeek command.academicYear command.dept />)

							<#list pointFields as pointField>
								<@spring.bind path="command.monitoringPoints[${pointCount}].${pointField}">
									<#if status.error>
										<div class="alert alert-error"><@f.errors path="command.monitoringPoints[${pointCount}].${pointField}" cssClass="error"/></div>
									</#if>
								</@spring.bind>
							</#list>

							<#include "../point/_hidden_fields.ftl" />
						</div>
					</div>
					<#assign pointCount = pointCount + 1 />
				</#list>
			</div>
		</div>
	</#macro>

	<#if command.monitoringPoints?size == 0>
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