<#compress><#escape x as x?html>

<#import "*/permissions_macros.ftl" as pm />
<#assign perms_url></#assign>

<div class="permissions-page">
	<#-- Permissions parents -->
	<#if target.permissionsParents?size gt 0 || target.customRoleDefinitions??>
		<div class="pull-right">
			<div>
				<#if target.customRoleDefinitions??>
					<a class="btn" href="<@routes.customroles target />">
						<i class="icon-user"></i> Custom roles
					</a>
				</#if>

				<#if target.permissionsParents?size gt 0>
					<div class="btn-group">
						<button class="btn dropdown-toggle" data-toggle="dropdown">
							Permissions parents <span class="caret"></span>
						</button>
						<ul class="dropdown-menu">
							<#list target.permissionsParents as parent>
								<li><a href="<@routes.permissions parent />"><i class="icon-lock"></i> ${parent.humanReadableId}</a></li>
							</#list>
						</ul>
					</div>
				</#if>
			</div>
			<br>
			<div class="pull-right"><a href="<@routes.roles />"><strong>About roles</strong></a></div>
		</div>
	</#if>

	<h1 class="with-settings">Permissions</h1>
	<h5><span class="muted">on</span> ${target.humanReadableId}</h5>

	<#-- Alerts -->

	<@pm.alerts "addCommand" target.humanReadableId users role />

	<div class="existing-roles">
		<h2>Granted roles</h2>

		<#if existingRoleDefinitions?size gt 0>
			<#list existingRoleDefinitions?keys?chunk(2) as roleDefinitions>
				<div class="row-fluid">
					<#list roleDefinitions as roleDefinition>
						<#assign roleDefinitionName><#compress>
							<#if roleDefinition.id??>
								${roleDefinition.id}
							<#elseif roleDefinition.selector??>
								${roleDefinition.name}(${roleDefinition.selector.id})
							<#else>
								${roleDefinition.name}
							</#if>
						</#compress></#assign>

						<div class="span6">
							<h3 class="permissionTitle">${roleDefinition.description}</h3>
							<#assign roleDescription><@pm.debugRole role=mapGet(existingRoleDefinitions, roleDefinition) showScopes=false /></#assign>
							<a class="use-popover colour-h3"
							   id="popover-${roleDefinition.name}"
							   data-html="true"
							   data-original-title="${roleDefinition.description}"
							   data-content="${roleDescription}"><i class="icon-question-sign"></i></a>

							<@pm.roleTable perms_url "${roleDefinition.name}-table" target roleDefinitionName roleDefinition.description />
						</div>
					</#list>
				</div>
			</#list>
		<#else>
			<p>There are no granted roles applied against ${target.humanReadableId}.</p>
		</#if>
	</div>

	<#-- Add a new (pre-existing) role -->
	<div class="new-role">
		<h2>Grant a new role</h2>

		<#if grantableRoleDefinitions?size gt 0>
			<#list grantableRoleDefinitions?keys?chunk(2) as roleDefinitions>
				<div class="row-fluid">
					<#list roleDefinitions as roleDefinition>
						<#assign roleDefinitionName><#compress>
							<#if roleDefinition.id??>
								${roleDefinition.id}
							<#elseif roleDefinition.selector??>
								${roleDefinition.name}(${roleDefinition.selector.id})
							<#else>
								${roleDefinition.name}
							</#if>
						</#compress></#assign>

						<div class="span6">
							<h3 class="permissionTitle">${roleDefinition.description}</h3>
							<#assign roleDescription><@pm.debugRole role=mapGet(grantableRoleDefinitions, roleDefinition) showScopes=false /></#assign>
							<a class="use-popover colour-h3"
							   id="popover-${roleDefinition.name}"
							   data-html="true"
							   data-original-title="${roleDefinition.description}"
							   data-content="${roleDescription}"><i class="icon-question-sign"></i></a>

							<@pm.roleTable perms_url "${roleDefinition.name}-table" target roleDefinitionName roleDefinition.description />
						</div>
					</#list>
				</div>
			</#list>
		<#else>
			<p>You do not have permission to grant any roles against ${target.humanReadableId}.</p>
		</#if>
	</div>

	<#-- Existing permissions -->
	<div class="existing-permissions">
		<h2>Explicitly granted/revoked permissions</h2>

		<#if existingPermissions?size gt 0>
			<table class="permissions-table table table-bordered permission-list">
				<thead>
					<th>User</th>
					<th>Permission</th>
					<th>Allowed?</th>
					<th>&nbsp;</th>
				</thead>
				<tbody>
					<#list existingPermissions as grantedPermission>
						<#assign permissionName><#compress>
							<#if grantedPermission.permission.selector??>
								${grantedPermission.permission.name}(${grantedPermission.permission.selector.id})
							<#else>
								${grantedPermission.permission.name}
							</#if>
						</#compress></#assign>
						<@manageRole scope=target permissionName=permissionName>
							<#list grantedPermission.users.users as u>
								<tr>
									<td class="user">
										${u.fullName} <span class="muted">${u.userId}</span>
									</td>
									<td><abbr title="${grantedPermission.permission.description}" class="initialism">${permissionName}</abbr></td>
									<td>
										<#if grantedPermission.overrideType>
											<i class="icon-ok" title="Allowed"></i>
										<#else>
											<i class="icon-remove" title="Not allowed"></i>
										</#if>
									</td>
									<td class="actions">
										<#if can_delegate>
											<form action="${perms_url}" method="post" class="remove-permissions" onsubmit="return confirm('Are you sure you want to remove permission for this user?');">
												<input type="hidden" name="_command" value="removeSingle">
												<input type="hidden" name="permission" value="${permissionName}">
												<input type="hidden" name="overrideType" value="<#if grantedPermission.overrideType>true<#else>false</#if>">
												<input type="hidden" name="usercodes" value="${u.userId}">
												<a class="btn btn-danger btn-mini removeUser"><i class="icon-white icon-remove"></i></a>
											</form>
										<#else>
											<#assign popoverText>
												<p>You can't remove ${permissionName} because you don't have permission to:</p>
												<ul>
													<#list denied_permissions as perm>
														<li>${perm.description}</li>
													</#list>
												</ul>
												<p>on ${target.toString}.</p>
											</#assign>

											<button class="btn btn-danger btn-mini use-tooltip disabled" type="button"
													data-html="true"
													data-title="${popoverText}"><i class="icon-white icon-remove"></i></button>
										</#if>
									</td>
								</tr>
							</#list>
						</@manageRole>
					</#list>
				</tbody>
			</table>
		<#else>
			<p>There are no granted or revoked permissions applied against ${target.humanReadableId}.</p>
		</#if>
	</div>

	<#-- Add a new permission -->
	<div class="add-permission well">
		<h2>Explicitly grant or revoke permissions</h2>

		<@f.form action="${perms_url}" cssClass="form-horizontal" commandName="addSingleCommand">
			<@f.errors cssClass="error form-errors" />

			<input type="hidden" name="_command" value="addSingle">

			<fieldset>
				<@form.labelled_row "usercodes" "User ID">
					<@form.flexipicker path="usercodes" placeholder="Type name or usercode" />
				</@form.labelled_row>

				<@form.labelled_row "permission" "Permission">
					<@f.select path="permission">
						<#list allPermissions?keys as group>
							<optgroup label="${group}">
								<#list allPermissions[group] as permission>
									<option value="${permission._1()}"<#if status.value?? && permission._1()?? && (status.value!"") == (permission._1()!"")> selected="selected"</#if>>${permission._2()}</option>
								</#list>
							</optgroup>
						</#list>
					</@f.select>
				</@form.labelled_row>

				<@form.labelled_row "overrideType" "Allowed?">
					<@f.select path="overrideType">
						<option value="true">Allow</option>
						<option value="false">Disallow</option>
					</@f.select>
				</@form.labelled_row>
			</fieldset>

			<div class="submit-buttons">
				<button type="submit" class="btn"><i class="icon-plus"></i> Add</button>
			</div>
		</@f.form>
	</div>
</div>

<@pm.script />

</#escape></#compress>