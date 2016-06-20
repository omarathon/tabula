<#import "../attendance_macros.ftl" as attendance_macros />

<@fmt.deptheader "Manage monitoring points" "for" command.dept routes.attendance "manageDepartment" "with-settings" />

<#if !dept.routes?has_content && dept.children?has_content>
	<p class="alert alert-info"><i class="icon-info-sign"></i> This department doesn't directly contain any routes. Check subdepartments.</p>
<#else>
<script type="text/javascript">
	var setsByRouteByAcademicYear = {
		<#list command.setsByRouteByAcademicYear?keys as academicYear>
			"${academicYear}" : [
				<#list command.sortedRoutesByAcademicYear(academicYear) as route>
					{
						"code" : "${route.code}",
						"name" : "${route.name}",
						"sets" : [
							<#list command.setsByRouteCodeByAcademicYear(academicYear, route) as set>
								{
									"id" : "${set.id}",
									"year" : "<#if set.year??>${set.year}<#else>All</#if>"
								}
								<#if set_has_next>,</#if>
							</#list>
						]
					}
					<#if route_has_next>,</#if>
				</#list>
			]
			<#if academicYear_has_next>,</#if>
		</#list>
	};
</script>

<#if createdCount?? >
	<div class="alert alert-success">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		Monitoring schemes for ${createdCount} route<#if (createdCount > 1)>s</#if> have been created
	</div>
</#if>

<form id="chooseCreateType" class="form-inline" action="<@routes.attendance.createSet command.dept command.academicYear />">
	<h2>Create monitoring schemes</h2>
	<label>
		<input class="create blank" type="radio" checked name="createType" value="blank"/>
		Create blank scheme
		<a class="use-popover" id="popover-create-blank" data-content="Create a new scheme from scratch"><i class="icon-question-sign"></i></a>
	</label>
	<select style="visibility:hidden"></select>
	<br/>
	<#if (templates?size > 0)>
		<label>
			<input class="create template" type="radio" name="createType" value="template"/>
			Create from approved template
			<a class="use-popover" id="popover-create-template" data-content="Choose a template monitoring scheme developed for each year of study"><i class="icon-question-sign"></i></a>
		</label>
		<span class="existingSetOptions">
			<select name="template" class="template">
				<#list templates as template>
					<option value="${template.id}">${template.templateName}</option>
				</#list>
			</select>
			<a class="btn monitoring-point-preview-button ajax-modal" data-target="#monitoring-point-preview-modal" href="#" data-hreftemplate="/attendance/monitoringpoints/preview/template/_TEMPLATE_ID_?department=${command.dept.code}&academicYear=${command.thisAcademicYear.storeValue?c}">
				Preview&hellip; <#-- wired by class in js -->
			</a>
		</span>
		<br />
	</#if>
	<#if (command.setsByRouteByAcademicYear?keys?size > 0)>
		<label>
			<input class="create copy" type="radio" name="createType" value="copy"/>
			Copy an existing scheme in your department
			<a class="use-popover" id="popover-create-copy" data-content="Choose an existing scheme to copy by academic year, route, and year of study"><i class="icon-question-sign"></i></a>
		</label>
		<select class="academicYear input-medium">
			<option style="display:none;" disabled selected value="">Academic year</option>
		</select>
		<select class="route input-xlarge">
			<option style="display:none;" disabled selected value="">Route</option>
		</select>
		<select name="existingSet" class="input-medium copy">
			<option style="display:none;" disabled selected value="">Year of study</option>
		</select>
	<#else>
		<label>
			<input class="create copy" type="radio" disabled name="createType"/>
			<span class="hint">Copy an existing scheme</span>
			<a class="use-popover" id="popover-create-copy-disabled" data-content="There are no existing monitoring schemes for your department"><i class="icon-question-sign"></i></a>
        </label>
	</#if>
	<br/>
	<button type="submit" class="btn btn-primary">Create</button>
</form>

<h2>Edit monitoring points</h2>

<#if command.setsByRouteByAcademicYear?keys?size == 0>
	<p><em>There are no monitoring point schemes for ${command.dept.name}</em></p>
<#elseif !command.setsByRouteByAcademicYear[command.academicYear.toString]??>
	<p><em>There are no monitoring point schemes for ${command.dept.name} for the selected academic year</em></p>
<#else>
	<div class="striped-section routes">
		<div class="row-fluid">
			<div class="span10">
				<h3 class="section-title">Route</h3>
			</div>
			<div class="span2">
				<h3 class="section-title">Years</h3>
			</div>
		</div>

		<div class="striped-section-contents">
			<#list command.setsByRouteByAcademicYear[command.academicYear.toString]?keys?sort_by("code") as route>
				<#assign pointSets = command.setsByRouteCodeByAcademicYear(command.academicYear.toString, route) />
				<div class="item-info">
					<div class="row-fluid">
						<div class="span10 collapsible">
							<h3><i class="icon-fixed-width icon-chevron-right ellipsis"></i> <@fmt.route_name route /></h3>
						</div>
						<div class="span2">
							<#if pointSets?size == 1 && !pointSets?first.year??>
								All
							<#else>
								<#list pointSets?sort_by("year") as set>${set.year}<#if set_has_next>, </#if></#list>
							</#if>
						</div>
					</div>
					<div class="collapsible-target">
						<#if pointSets?size == 1 && !pointSets?first.year??>
							<#assign set=pointSets?first />
							<div class="row-fluid">
								<div class="span10">
									${route.code?upper_case} All years
								</div>
								<div class="span2">
									<a href="<@routes.attendance.editSet set />" class="btn btn-primary btn-mini">Edit</a>
								</div>
							</div>
						<#else>
							<#list pointSets?sort_by("year") as set>
								<div class="row-fluid">
									<div class="span10">
										${route.code?upper_case} Year ${set.year}
									</div>
									<div class="span2">
										<a href="<@routes.attendance.editSet set />" class="btn btn-primary btn-mini">Edit</a>
									</div>
								</div>
							</#list>
						</#if>
					</div>
				</div>
			</#list>
		</div>
	</div>
</#if>

<div id="monitoring-point-preview-modal" class="modal hide fade" style="display:none;">
</div>
</#if>
