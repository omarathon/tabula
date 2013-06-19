<#-- Common template parts for use in other small groups templates. -->

<#function module_anchor module>
	<#return "module-${module.code}" />
</#function>


<#-- module_info: currently being built for a tutor's view of small groups,
   but when finished will also be used in admin/department.ftl which has a very
   similar rendition of modules and small groups. -->
<#macro module_info data can_manage_dept=false>
<#list data.moduleItems as moduleItem>

<#assign module=moduleItem.module />
<#assign can_manage=moduleItem.canManage />
<#assign has_groups=(moduleItem.setItems!?size gt 0) />
<#assign has_archived_groups=false />
<#list moduleItem.setItems as setItem>
	<#if setItem.set.archived>
		<#assign has_archived_groups=true />
	</#if>
</#list>

<a id="${module_anchor(module)}"></a>
<div class="striped-section<#if has_groups> collapsible expanded</#if><#if can_manage_dept && !has_groups> empty</#if>"
     data-name="${module_anchor(module)}">
    <div class="clearfix">
        <div class="btn-group section-manage-button">
            <a class="btn btn-medium dropdown-toggle" data-toggle="dropdown"><i class="icon-wrench"></i> Manage <span
                    class="caret"></span></a>
            <ul class="dropdown-menu pull-right">
				<#if can_manage>
                    <li><a href="<@routes.moduleperms module />">
                        <i class="icon-user icon-fixed-width"></i> Edit module permissions
                    </a></li>
				</#if>

                <li><a href="<@routes.createset module />"><i class="icon-group icon-fixed-width"></i> Add small groups</a>
                </li>

				<#if has_archived_groups>
                    <li><a class="show-archived-small-groups" href="#">
                        <i class="icon-eye-open icon-fixed-width"></i> Show archived small groups
                    </a>
                    </li>
				</#if>

            </ul>
        </div>

        <h2 class="section-title with-button"><@fmt.module_name module /></h2>
    </div>
</div>

</#list>
</#macro>