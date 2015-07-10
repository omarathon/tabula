<#escape x as x?html>
	<h1>Custom roles</h1>

	<p>Custom roles can be created here and then applied to any object that belongs to this department.</p>

	<#if !customRoles?has_content>
		<p>No custom roles have been created yet. Click <strong>Create</strong> below to make one.</p>
	</#if>

	<p><a class="btn" href="<@routes.admin.addcustomrole department />"><i class="icon-plus fa fa-plus"></i> Create</a></p>

	<#if customRoles?has_content>
		<table class="table table-bordered table-striped custom-roles">
			<thead>
				<tr>
					<th>Name</th>
					<th>Base definition</th>
					<th></th>
				</tr>
			</thead>
			<tbody>
				<#list customRoles as info>
					<#assign customRoleDefinition = info.customRoleDefinition />
					<#assign canDelete = (info.grantedRoles == 0 && info.derivedRoles == 0) />
					<tr>
						<td>${customRoleDefinition.name}</td>
						<td>
							${customRoleDefinition.baseRoleDefinition.description}
							<a class="btn btn-mini" href="<@routes.admin.customroleoverrides customRoleDefinition />"><i class="icon-edit fa fa-pencil-square-o"></i> +<@fmt.p customRoleDefinition.overrides?size "override"/></a>
						</td>
						<td>
							<a class="btn btn-mini" href="<@routes.admin.editcustomrole customRoleDefinition />"><i class="icon-edit fa fa-pencil-square-o"></i> Modify</a>
							<a class="btn btn-mini btn-danger<#if !canDelete> use-tooltip disabled</#if>" href="<@routes.admin.deletecustomrole customRoleDefinition />" data-toggle="modal" data-target="#custom-roles-modal"<#if !canDelete> title="You can't delete this custom role as it is in use by <@fmt.p info.grantedRoles "granted role" /> and <@fmt.p info.derivedRoles "derived role definition" />."</#if>><i class="icon-remove fa fa-times icon-white fa fa-inverse"></i> Delete</a>
						</td>
					</tr>
				</#list>
			</tbody>
		</table>
	</#if>

	<div id="custom-roles-modal" class="modal fade">
		<div class="modal-header">
			<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
			<h3>Delete custom role</h3>
		</div>
		<div class="modal-body"></div>
	</div>

	<script type="text/javascript">
		jQuery(function($){

			$('.custom-roles').on('click', 'a[data-toggle=modal]', function(e){
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