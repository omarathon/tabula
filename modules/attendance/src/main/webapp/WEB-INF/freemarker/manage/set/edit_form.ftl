<#escape x as x?html>

<h1>Edit monitoring scheme ${command.set.route.code?upper_case} ${command.set.route.name}, <#if command.set.year??>Year ${command.set.year}<#else>All years</#if></h1>

<#if command.set.sentToAcademicOffice>
	<div class="alert alert-block">
		<h4>Monitoring points cannot be changed for this scheme</h4>
		The monitoring points in this scheme cannot be changed as the attendance information has already been submitted.
	</div>
</#if>

<div class="modify-monitoring-points">
	<div class="row-fluid">
		<div class="span3">
			<h2>Monitoring points</h2>
		</div>
		<div class="span9">
			<a href="<@url page="/manage/${command.set.route.department.code}/sets/${command.set.id}/points/add" />" class="btn btn-primary new-point <#if command.set.sentToAcademicOffice>disabled</#if>">
				<i class="icon-plus"></i>
				Create new point
			</a>
		</div>
	</div>

	<#include "_monitoringPointsPersisted.ftl" />
</div>

<a class="btn" href="<@url page="/manage/${command.set.route.department.code}"/>">Done</a>

<div id="modal" class="modal hide fade" style="display:none;"></div>

</#escape>