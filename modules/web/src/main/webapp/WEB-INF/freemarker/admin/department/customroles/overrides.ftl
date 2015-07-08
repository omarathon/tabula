<#import "*/permissions_macros.ftl" as pm />

<#escape x as x?html>
	<h1>${customRoleDefinition.description}</h1>

	<div class="overrides">
		<h2>Role overrides</h2>

		<p><a class="btn" href="<@routes.admin.addcustomroleoverride customRoleDefinition />"><i class="icon-plus fa fa-plus"></i> Create</a></p>

		<#if roleInfo.overrides?size gt 0>
			<table class="permissions-table table table-bordered permission-list">
				<thead>
					<th>Permission</th>
					<th>Allowed?</th>
					<th>&nbsp;</th>
				</thead>
				<tbody>
					<#list roleInfo.overrides as override>
						<#assign permissionName><#compress>
							<#if override.permission.selector??>
								${override.permission.name}(${override.permission.selector.id})
							<#else>
								${override.permission.name}
							</#if>
						</#compress></#assign>
						<tr>
							<td><abbr title="${override.permission.description}" class="initialism">${permissionName}</abbr></td>
							<td>
								<#if override.overrideType>
									<i class="icon-ok fa fa-check" title="Allowed"></i>
								<#else>
									<i class="icon-remove fa fa-times" title="Not allowed"></i>
								</#if>
							</td>
							<td class="actions">
								<a class="btn btn-mini btn-danger" href="<@routes.admin.deletecustomroleoverride override />" data-toggle="modal" data-target="#custom-roles-modal"><i class="icon-remove fa fa-times icon-white fa fa-inverse"></i> Delete</a>
							</td>
						</tr>
					</#list>
				</tbody>
			</table>
		<#else>
			<p>There are no overrides for ${customRoleDefinition.name}.</p>
		</#if>
	</div>

	<h2>All permissions</h2>

	<@pm.debugRole role=roleInfo.role showScopes=false />

	<div id="custom-roles-modal" class="modal fade">
		<div class="modal-header">
			<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
			<h3>Delete role override</h3>
		</div>
		<div class="modal-body"></div>
	</div>

	<script type="text/javascript">
		jQuery(function($){
			$('.permission-list').on('click', 'a[data-toggle=modal]', function(e){
				var $this = $(this);
				var $modal = $($this.data('target'));
				var $body = $modal.find('.modal-body').empty();
				$body.load($this.attr('href'), function() {
					$body.find('.btn').each(function() {
						if ($(this).text() == 'Cancel') {
							$(this).attr('data-dismiss', 'modal');
						}
					});
				});
			});

			$("a.disabled").on('click', function(e){e.preventDefault(e); return false;})
		});
	</script>
</#escape>