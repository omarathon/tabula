<#macro debugRole role>
  <span class="role"><i class="fal fa-user"></i> ${role.definition.description}</span>
  <#if role.explicitPermissions?size gt 0 || role.subRoles?size gt 0>
    <ul>
      <#list role.subRoles as subRole>
        <li><@debugRole role=subRole /></li>
      </#list>
      <#list role.viewablePermissionsAsList as permission>
        <li><@debugPermission permission=permission._1() /></li>
      </#list>
    </ul>
  </#if>
</#macro>

<#macro debugPermission permission>
  <span class="permission"><i tabindex="0" class="fal fa-lock use-tooltip" title="${permission.name}"></i> ${permission.description}</span>
</#macro>

<#macro roleTable cssClass roleDefinition roleName>
  <div class="permission-list ${cssClass}">
    <div class="row">
      <div class="col-md-12">
        <@f.form method="post" cssClass="add-permissions" modelAttribute="grantRoleCommand">
          <@f.errors cssClass="error form-errors" />

          <input type="hidden" name="_action" value="grantRole">
          <input type="hidden" name="roleDefinition" value="${roleDefinition}">

          <@bs3form.flexipicker cssClass="pickedUser" path="usercodes" staffOnly="true" placeholder="Enter name or ID">
            <span class="input-group-btn">
              <button class="btn btn-default" type="submit">Add</button>
            </span>
          </@bs3form.flexipicker>
          <@f.errors path="usercodes" cssClass="text-danger" />
        </@f.form>
      </div>
    </div>
    <#local userGroup = (mapGet(globalGrantedRoles, roleDefinition?markup_string).users)!{} />
    <#local users = (userGroup.users)![] />
    <#local userGroupItems = (userGroup.items)![] />
    <#if users?size gt 0>
      <#list users as u>
        <#local userGroupItemType = 'Included' />
        <#list userGroupItems?reverse as item>
          <#if item._1().userId == u.userId>
            <#local userGroupItemType = item._2().toString />
          </#if>
        </#list>

        <div class="row">
          <div class="col-md-12">
            <div class="col-md-12 <#if u_has_next>user</#if>">
              <div class="pull-right">
                <#if userGroupItemType != 'WebGroup'>
                  <@f.form method="post" cssClass="remove-permissions"
                        data\-confirm\-submit="Are you sure you want to remove permission for this user?"
                        modelAttribute="revokeRoleCommand">
                    <input type="hidden" name="_action" value="revokeRole">
                    <input type="hidden" name="roleDefinition" value="${roleDefinition}">
                    <input type="hidden" name="usercodes" value="${u.userId}">
                    <button type="submit" class="btn btn-danger btn-xs removeUser">Remove</button>
                  </@f.form>
                <#else>
                  <button class="btn btn-danger btn-xs tabula-tooltip disabled" type="button"
                          data-title="You can't remove ${u.fullName} because their permissions come from WebGroups (${userGroup.baseWebgroup})">
                    Remove
                  </button>
                </#if>
              </div>
              ${u.fullName} <span class="very-subtle">${u.userId}</span>
            </div>
          </div>
        </div>
      </#list>
    <#else>
      <div class="row">
        <div class="col-md-12">
          There is no ${roleName} yet.
        </div>
      </div>
    </#if>
  </div>
</#macro>

<div class="permissions-page">
  <h1>Global permissions</h1>

  <div class="existing-roles">
    <h2>Granted roles</h2>

    <#if existingRoleDefinitions?size gt 0>
      <#list existingRoleDefinitions?keys?chunk(2) as roleDefinitions>
        <div class="row">
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

            <div class="col-md-6">
              <#assign roleDescription><@debugRole role=mapGet(existingRoleDefinitions, roleDefinition) /></#assign>
              <h3 class="permissionTitle">${roleDefinition.description} <@fmt.help_popover id="${roleDefinition.name}" title="${roleDefinition.description}" content=roleDescription?markup_string html=true /></h3>

              <@roleTable "${roleDefinition.name}-table" roleDefinitionName roleDefinition.description />
            </div>
          </#list>
        </div>
      </#list>
    <#else>
      <p>There are no globally granted roles applied.</p>
    </#if>
  </div>

  <#-- Add a new (pre-existing) role -->
  <div class="new-role">
    <h2>Grant a new role</h2>

    <#if grantableRoleDefinitions?size gt 0>
      <#list grantableRoleDefinitions?keys?chunk(2) as roleDefinitions>
        <div class="row">
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

            <div class="col-md-6">
              <#assign roleDescription><@debugRole role=mapGet(grantableRoleDefinitions, roleDefinition) /></#assign>
              <h3 class="permissionTitle">${roleDefinition.description} <@fmt.help_popover id="${roleDefinition.name}" title="${roleDefinition.description}" content=roleDescription?markup_string html=true /></h3>

              <@roleTable "${roleDefinition.name}-table" roleDefinitionName roleDefinition.description />
            </div>
          </#list>
        </div>
      </#list>
    <#else>
      <p>There are no roles to grant globally.</p>
    </#if>
  </div>

  <#-- Existing permissions -->
  <div class="existing-permissions">
    <h2>Explicitly granted/revoked permissions</h2>

    <#if existingPermissions?size gt 0>
      <table class="table permission-list">
        <thead>
          <tr>
            <th>User</th>
            <th>Permission</th>
            <th>Allowed?</th>
            <th>&nbsp;</th>
          </tr>
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

            <#assign userGroup = grantedPermission.users />
            <#assign userGroupItems = (userGroup.items)![] />
            <#list userGroup.users as u>
              <#assign userGroupItemType = 'Included' />
              <#list userGroupItems?reverse as item>
                <#if item._1().userId == u.userId>
                  <#assign userGroupItemType = item._2().toString />
                </#if>
              </#list>

              <tr>
                <td class="user">
                  ${u.fullName} <span class="very-subtle">${u.userId}</span>
                </td>
                <td><abbr title="${grantedPermission.permission.description}" class="initialism">${permissionName}</abbr></td>
                <td>
                  <#if grantedPermission.overrideType>
                    <i class="fa fa-check" title="Allowed"></i>
                  <#else>
                    <i class="fa fa-times" title="Not allowed"></i>
                  </#if>
                </td>
                <td class="actions">
                  <#if userGroupItemType != 'WebGroup'>
                    <@f.form method="post" class="remove-permissions"
                          data\-confirm\-submit="Are you sure you want to remove permission for this user?"
                          modelAttribute="revokePermissionsCommand">
                      <input type="hidden" name="_action" value="revokePermissions">
                      <input type="hidden" name="permission" value="${permissionName}">
                      <input type="hidden" name="overrideType" value="<#if grantedPermission.overrideType>true<#else>false</#if>">
                      <input type="hidden" name="usercodes" value="${u.userId}">
                      <button aria-label="Remove user" type="submit" class="btn btn-danger btn-xs removeUser"><i class="fa fa-inverse fa-times"></i></button>
                    </@f.form>
                  <#else>
                    <button class="btn btn-danger btn-xs tabula-tooltip disabled" type="button"
                            data-title="You can't remove ${u.fullName} because their permissions come from WebGroups (${userGroup.baseWebgroup})">
                      <i class="fa fa-inverse fa-times"></i>
                    </button>
                  </#if>
                </td>
              </tr>
            </#list>
          </#list>
        </tbody>
      </table>
    <#else>
      <p>There are no granted or revoked permissions applied against ${display_name}.</p>
    </#if>
  </div>

  <#-- Add a new permission -->
  <div class="add-permission well">
    <h2>Explicitly grant or revoke permissions</h2>

    <@f.form modelAttribute="grantPermissionsCommand">
      <@f.errors cssClass="error form-errors" />

      <input type="hidden" name="_action" value="grantPermissions">

      <@bs3form.labelled_form_group path="usercodes" labelText="User ID">
        <@bs3form.flexipicker path="usercodes" placeholder="Type name or usercode" />
      </@bs3form.labelled_form_group>

      <@bs3form.labelled_form_group path="permission" labelText="Permission">
        <@f.select path="permission" cssClass="form-control">
          <#list allPermissions?keys as group>
            <optgroup label="${group}">
              <#list allPermissions[group] as permission>
                <option value="${permission._1()}"<#if status.value?? && permission._1()?? && (status.value!"") == (permission._1()!"")> selected="selected"</#if>>${permission._2()}</option>
              </#list>
            </optgroup>
          </#list>
        </@f.select>
      </@bs3form.labelled_form_group>

      <@bs3form.labelled_form_group path="overrideType" labelText="Allowed?">
        <@f.select path="overrideType" cssClass="form-control">
          <option value="true">Allow</option>
          <option value="false">Disallow</option>
        </@f.select>
      </@bs3form.labelled_form_group>

      <div class="submit-buttons">
        <button type="submit" class="btn btn-default">Add</button>
      </div>
    </@f.form>
  </div>
</div>
