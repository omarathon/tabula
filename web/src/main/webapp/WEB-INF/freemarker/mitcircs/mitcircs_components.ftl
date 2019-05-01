<#ftl strip_text=true />

<#macro enumListWithOther enumValues otherValue>
  <#list enumValues as value>${value.description}<#if value.entryName == "Other"> (${otherValue})</#if><#if value_has_next>, </#if></#list>
</#macro>

<#macro detail label>
  <div class="row form-horizontal">
    <div class="col-sm-3 control-label">
      ${label}
    </div>
    <div class="col-sm-9">
      <div class="form-control-static">
        <#nested>
      </div>
    </div>
  </div>
</#macro>

<#macro section label>
  <div class="mitcircs-details__section row form-horizontal">
    <div class="control-label">${label}</div>
    <div class="content form-control-static">
      <#nested>
    </div>
  </div>
</#macro>

<#macro asyncSection id label url>
  <div id="${id}" class="mitcircs-details__section async row form-horizontal" data-href="${url}">
    <div class="control-label">${label}</div>
    <div class="content form-control-static">
      <i class="fas fa-spinner fa-pulse"></i> Loading&hellip;
    </div>
  </div>
</#macro>