<h1>Monitoring points list</h1>


<ul>
	<#list monitoringPoints as monitoringPoint>
		<li><a href="#">${monitoringPoint.name}</a> - ${monitoringPoint.week}</li>
	</#list>

</ul>
