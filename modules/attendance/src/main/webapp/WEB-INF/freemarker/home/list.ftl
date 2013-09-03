<h1>Monitoring points list</h1>

<#if updatedMonitoringPoint?? >
	<div class="alert alert-success">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		You updated checkpoints for <strong>${updatedMonitoringPoint.name}</strong>
	</div>
</#if>

<table class="monitoringPointsList table table-bordered table-striped">
	<thead>
		<tr>
			<th>Monitoring point</th>
			<th>Week number</th>
		</tr>
	</thead>
	<tbody class="striped-section-contents">
		<#list monitoringPoints as monitoringPoint>
			<tr>
				<td><a href="monitoringpoints/${monitoringPoint.id}/week/${monitoringPoint.week}/set?returnTo=/monitoringpoints<#if (RequestParameters.page??)>?page=${RequestParameters.page}</#if>">${monitoringPoint.name}</a></td>
				<td>${monitoringPoint.week}</td>
			</tr>
		</#list>
	</tbody>
</table>
