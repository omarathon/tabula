<#escape x as x?html>

<h1>Edit monitoring scheme <@fmt.route_name command.set.route />, <#if command.set.year??>Year ${command.set.year}<#else>All years</#if></h1>

<div class="modify-monitoring-points">
	<div class="row-fluid">
		<div class="span4">
			<h2>Monitoring points</h2>
		</div>
		<div class="span8">
			<a href="<@routes.attendance.createPoint command.set />" class="btn btn-primary new-point">
				<i class="icon-plus"></i>
				Create new point
			</a>
		</div>
	</div>

	<#include "_monitoringPointsPersisted.ftl" />
</div>

<a class="btn" href="<@routes.attendance.manageDepartment command.set.route.adminDepartment />">Done</a>

<div id="modal" class="modal hide fade" style="display:none;"></div>

</#escape>