<#ftl strip_text=true />

<#-- FIXME why is this necessary? -->
<#if JspTaglibs??>
  <#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
  <#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
</#if>

<#macro enumListWithOther enumValues otherValue>
  <#list enumValues as value>${value.description}<#if value.entryName == "Other"> (${otherValue?trim})</#if><#if value_has_next>, </#if></#list>
</#macro>

<#macro detail label condensed=false>
  <div class="row form-horizontal mitcircs-details__detail <#if condensed>mitcircs-details__detail--condensed</#if>">
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

<#-- Progress bar for a single submission  -->
<#macro stage_progress_bar stages>
  <div class="stage-progress-bar">
    <#list stages as progress>
      <#local stage = progress.stage />

      <#local state = 'default' />
      <#local icon = 'fa-circle-o' />
      <#if progress.completed>
        <#local state = 'success' />
        <#local icon = 'fa-check-circle-o' />
      <#elseif progress.skipped>
        <#local state = 'primary' />
        <#local icon = 'fa-arrow-circle-o-right' />
      <#elseif progress.started>
        <#local state = 'warning' />
        <#local icon = 'fa-dot-circle-o' />
      </#if>

      <#local title><@spring.message code=progress.stage.actionCode /></#local>
      <#if progress_index gt 0>
        <div class="bar bar-${state} tabula-tooltip" data-title="${fmt.strip_html(title)}"></div>
      </#if>
      <#local title><@spring.message code=progress.messageCode /></#local>
      <span class="fa-stack tabula-tooltip" data-title="${fmt.strip_html(title)}">
				<i class="fa fa-stack-1x fa-circle fa-inverse"></i>
				<i class="fa fa-stack-1x ${icon} text-${state}"></i>
			</span>
    </#list>
  </div>
</#macro>

<#macro attachments submission>
  <#if submission.attachments?has_content>
    <ul class="unstyled">
      <#list submission.attachments as attachment>
        <#local mimeTypeDetectionResult = mimeTypeDetector(attachment) />
        <li id="attachment-${attachment.id}" class="attachment">
          <@fmt.file_type_icon mimeTypeDetectionResult.mediaType />
          <a href="<@routes.mitcircs.renderAttachment submission attachment />" <#if mimeTypeDetectionResult.serveInline>data-inline="true"</#if>><#compress>${attachment.name}</#compress></a>
        </li>
      </#list>
    </ul>
  </#if>
</#macro>