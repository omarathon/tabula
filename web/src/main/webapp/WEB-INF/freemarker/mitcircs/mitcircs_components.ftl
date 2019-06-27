<#ftl strip_text=true />

<#-- FIXME why is this necessary? -->
<#if JspTaglibs??>
  <#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
  <#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
</#if>

<#macro enumListWithOther enumValues otherValue condensed=true><#compress>
  <#if condensed>
    <#list enumValues as value>${value.description}<#if value.entryName == "Other"> (${otherValue?trim})</#if><#if value_has_next>, </#if></#list>
  <#elseif enumValues?has_content>
    <ul class="list-unstyled">
      <#list enumValues as value>
        <li>${value.description}<#if value.entryName == "Other"> (${otherValue?trim})</#if></li>
      </#list>
    </ul>
  </#if>
</#compress></#macro>

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
    <table class="mitcircs-attachments table table-consensed">
      <thead>
        <tr>
          <th>File</th>
          <th>Date uploaded</th>
        </tr>
      </thead>
      <tbody>
        <#list submission.attachments as attachment>
          <#local mimeTypeDetectionResult = mimeTypeDetector(attachment) />
          <tr id="attachment-${attachment.id}" class="attachment mitcircs-attachments__attachment">
            <td>
              <@fmt.file_type_icon mimeTypeDetectionResult.mediaType />
              <a href="<@routes.mitcircs.renderAttachment submission attachment />" <#if mimeTypeDetectionResult.serveInline>data-inline="true"</#if>><#compress>${attachment.name}</#compress></a>
            </td>
            <td><@fmt.date date=attachment.dateUploaded /></td>
          </tr>
        </#list>
      </tbody>
    </table>
  </#if>
</#macro>

<#macro submissionTable submissions actions=true panel=false>
  <#if submissions?has_content>
    <table class="mitcircs-panel-form__submissions table table-condensed">
      <thead>
      <tr>
        <th>Reference</th>
        <th>Student</th>
        <th>Affected dates</th>
        <th>Last updated</th>
        <#if panel><th>Current panel</th></#if>
        <th></th>
      </tr>
      </thead>
      <tbody>
      <#list submissions as submission>
        <tr>
          <td>
            <#if !submission.draft>
              <a href="<@routes.mitcircs.reviewSubmission submission />">MIT-${submission.key}</a>
            <#else>
              MIT-${submission.key}
            </#if>
            <#if actions><@f.hidden path="submissions" value="${submission.key}" /></#if>
          </td>
          <td>
            <@pl.profile_link submission.student.universityId />
            ${submission.student.universityId}
            ${submission.student.firstName}
            ${submission.student.lastName}
          </td>
          <td>
            <@fmt.date date=submission.startDate includeTime=false relative=false />
            &mdash;
            <#if submission.endDate??>
              <@fmt.date date=submission.endDate includeTime=false relative=false />
            <#else>
              <span class="very-subtle">(ongoing)</span>
            </#if>
          </td>
          <td>
            <@fmt.date date=submission.lastModified />
            <#if submission.unreadByOfficer>
              <span class="tabula-tooltip" data-title="There are unread change(s)"><i class="far fa-envelope text-info"></i></span>
            </#if>
          </td>
          <#if panel>
            <td>
              <#if submission.panel??>
                ${submission.panel.name}
              <#else>
                <span class="very-subtle">None</span>
              </#if>
            </td>
          </#if>
          <#if actions>
            <td>
              <button class="remove btn btn-sm">Remove</button>
            </td>
          </#if>
        </tr>
      </#list>
      </tbody>
    </table>
  <#else>
    <div class="form-control-static">No submissions</div>
  </#if>
</#macro>

<#macro panelDetails panel show_name=false>
    <#if show_name>
      <@detail label="Name" condensed=true>
        <a href="<@routes.mitcircs.viewPanel panel />">${panel.name}</a>
      </@detail>
    </#if>
    <@detail label="Date" condensed=true>
        <#if panel.date??><@fmt.date date=panel.date includeTime=false relative=false />: <@fmt.time panel.startTime /> &mdash; <@fmt.time panel.endTime /><#else><span class="very-subtle">TBC</span></#if>
    </@detail>
    <#if panel.location??><@detail label="Location" condensed=true><@fmt.location panel.location /></@detail></#if>
    <@detail label="Panel chair" condensed=true>
        <#if panel.chair??>${panel.chair.fullName}<#else><span class="very-subtle">TBC</span></#if>
    </@detail>
    <@detail label="Panel secretary" condensed=true>
        <#if panel.secretary??>${panel.secretary.fullName}<#else><span class="very-subtle">TBC</span></#if>
    </@detail>
    <@detail label="Panel members" condensed=true>
        <#if panel.members?has_content>
            <#list panel.members as member>${member.fullName}<#if member_has_next>, </#if></#list>
        <#else>
          <span class="very-subtle">TBC</span>
        </#if>
    </@detail>
</#macro>

<#macro panelsTable panels>
  <table class="table table-condensed">
    <thead>
      <tr>
        <th>Name</th>
        <th>Date</th>
        <th>Location</th>
        <th>Chair</th>
        <th>Secretary</th>
        <th>Members</th>
        <th>Submissions</th>
      </tr>
    </thead>
    <tbody>
      <#list panels as panel>
        <tr>
          <td><a href="<@routes.mitcircs.viewPanel panel />">${panel.name}</a></td>
          <td>
            <#if panel.date??>
              <@fmt.date date=panel.date includeTime=false relative=false />: <@fmt.time panel.startTime /> &mdash; <@fmt.time panel.endTime />
            <#else>
                <span class="very-subtle">TBC</span>
            </#if>
          </td>
          <td><#if panel.location??><@fmt.location panel.location /></#if></td>
          <td><#if panel.chair??>${panel.chair.fullName}<#else><span class="very-subtle">TBC</span></#if></td>
          <td><#if panel.secretary??>${panel.secretary.fullName}<#else><span class="very-subtle">TBC</span></#if></td>
          <td><#list panel.members as member>${member.fullName}<#if member_has_next>, </#if></#list></td>
          <td><#if panel.submissions??>${panel.submissions?size}<#else>0</#if></td>
        </tr>
      </#list>
    </tbody>
  </table>
</#macro>