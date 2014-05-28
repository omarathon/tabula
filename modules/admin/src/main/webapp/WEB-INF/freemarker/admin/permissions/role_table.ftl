<#compress><#escape x as x?html>
	<style type="text/css">
		.roles-table th {
			vertical-align: middle !important;
		}

		.rotate {
			float: left; 
			position: relative;
			white-space: nowrap;
			-moz-transform: rotate(270deg);  /* FF3.5+ */        
			-o-transform: rotate(270deg);  /* Opera 10.5 */   
			-webkit-transform: rotate(270deg);  /* Saf3.1+, Chrome */              
			filter:  progid:DXImageTransform.Microsoft.BasicImage(rotation=3);  /* IE6,IE7 */          
			-ms-filter: progid:DXImageTransform.Microsoft.BasicImage(rotation=3); /* IE8 */
		}
	</style>

	<h1>Roles and capabilities</h1>

	<table class="table table-striped table-bordered table-condensed roles-table">
		<thead>
			<th></th>
			<#list (rolesTable?first)._2() as roles>
				<th><div class="rotate">${roles._1().description}</div></th>
			</#list>
		</thead>
		<tbody>
			<#list rolesTable as permission>
				<tr>
					<td>${permission._1().description}</td>
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
