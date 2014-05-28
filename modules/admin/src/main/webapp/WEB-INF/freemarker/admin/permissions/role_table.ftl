<#compress><#escape x as x?html>
	<style type="text/css">
		.roles-table td:first-child {
			text-align: right !important;
			white-space: nowrap;
		}
	</style>

	<h1>Roles and capabilities</h1>

	<table class="table table-striped table-bordered table-condensed roles-table">
		<thead>
			<th></th>
			<#list (rolesTable?first)._2() as roles>
				<th>${roles._1().description}</th>
			</#list>
		</thead>
		<tbody>
			<#list rolesTable as permission>
				<tr>
					<td><abbr title="${permission._1().description}">${permission._1().name}</abbr></td>
					<#list permission._2() as roles>
						<td>
							<#if roles._2()?has_content>
								<#if roles._2()>
									<i class="icon-ok attended"></i>
								<#else>
									<i class="icon-remove unauthorised"></i>
								</#if>
							<#else>
								<i class="icon-remove-circle authorised"></i>
							</#if>
						</td>
					</#list>
				</tr>
			</#list>
		</tbody>
	</table>

	<script type="text/javascript">
		jQuery(function($) {
			$('.roles-table th').each(function() {
				$(this).css('height', $(this).width()).css('width', 20);
			});
		});
	</script>
</#escape></#compress>
