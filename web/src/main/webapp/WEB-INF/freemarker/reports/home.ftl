<#escape x as x?html>
<h1>Reports</h1>

<p class="lead muted">
  This is a service for viewing reports of data used in Tabula.
</p>

<#if departmentsWithPermission?size == 0 && user.staff>
	<p>
		You do not currently have permission to view any reports. If you think this is incorrect or you need assistance, please visit our <a href="/help">help page</a>.
	</p>

	<script type="text/javascript">
		jQuery(function($) {
			$('#email-support-link').on('click', function(e) {
				e.stopPropagation();
				e.preventDefault();
				$('#app-feedback-link').click();
			});
		});
	</script>
</#if>

<#if departmentsWithPermission?has_content>
	<h2>View reports</h2>
	<ul>
		<#list departmentsWithPermission as department>
			<#list academicYears as year>
				<li><h3><a href="<@routes.reports.departmentWithYear department year />">${department.name} ${year.toString}</a></h3></li>
			</#list>
		</#list>
	</ul>
</#if>

</#escape>