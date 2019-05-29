<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/mitcircs_components.ftl" as components />
<#escape x as x?html>
  <div id="profile-modal" class="modal fade profile-subset"></div>
  <h1>${panel.name}</h1>
  <section class="mitcircs-details">
    <div class="row">
      <div class="col-sm-6 col-md-7">
        <@components.detail label="Date" condensed=true>
          <#if panel.date??><@fmt.date date=panel.date includeTime=false relative=false />: <@fmt.time panel.startTime /> &mdash; <@fmt.time panel.endTime /><#else><span class="very-subtle">TBC</span></#if>
        </@components.detail>
          <#if panel.location??><@components.detail label="Location" condensed=true><@fmt.location panel.location /></@components.detail></#if>
        <@components.detail label="Panel chair" condensed=true>
          <#if panel.chair??>${panel.chair.fullName}<#else><span class="very-subtle">TBC</span></#if>
        </@components.detail>
        <@components.detail label="Panel secretary" condensed=true>
          <#if panel.secretary??>${panel.secretary.fullName}<#else><span class="very-subtle">TBC</span></#if>
        </@components.detail>
        <@components.detail label="Panel members" condensed=true>
          <#if panel.members?has_content>
            <#list panel.members as member>${member.fullName}<#if member_has_next>, </#if></#list>
          <#else>
            <span class="very-subtle">TBC</span>
          </#if>
        </@components.detail>
      </div>
      <div class="col-sm-6 col-md-4">
        <div class="row form-horizontal">
          <div class="col-sm-4 control-label">Actions</div>
          <div class="col-sm-8">
            <p><a href="<@routes.mitcircs.home />" class="btn btn-default btn-block"><i class="fal fa-long-arrow-left"></i> Return to list of panels</a></p>
          </div>
        </div>
      </div>
    </div>
    <@components.section  label="Submissions">
      <@components.submissionTable submissions=panel.submissions![] actions=false panel=false />
    </@components.section>
  </section>
</#escape>