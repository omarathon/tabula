<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/mitcircs_components.ftlh" as components />

<#escape x as x?html>
  <div class="deptheader">
    <h1>Edit mitigating circumstances panel</h1>
  </div>

  <#if hasPanel?has_content || noPanel?has_content>
    <div class="alert alert-info">
      <i class="fa fa-info-sign fa fa-exclamation-triangle"></i>
      You are adding <@fmt.p (hasPanel?size + noPanel?size) "submission" /> to this panel. Your changes will not be recorded until you save.
    </div>
  </#if>

  <@f.form method="POST" modelAttribute="command" class="mitcircs-panel-form dirty-check double-submit-protection">
    <#include "_fields.ftl" />

    <@bs3form.labelled_form_group path="" labelText="Submissions in this panel">
      <@components.submissionTable thisPanel />
    </@bs3form.labelled_form_group>

    <#if hasPanel?has_content>
      <@bs3form.labelled_form_group path="" labelText="Submissions being moved to this panel">
        <p>The following <@fmt.p number=hasPanel?size singular="submission has" plural="submissions have" shownumber=false /> already been added to another panel. <@fmt.p number=hasPanel?size singular="It" plural="They" shownumber=false /> will be moved to this panel.</p>
        <@components.submissionTable submissions=hasPanel panel=true />
      </@bs3form.labelled_form_group>
    </#if>

    <#if noPanel?has_content>
      <@bs3form.labelled_form_group path="submissions" labelText="Submissions being added to this panel">
        <@components.submissionTable noPanel />
      </@bs3form.labelled_form_group>
    </#if>

    <div class="submit-buttons">
      <button type="submit" name="submit" class="btn btn-primary">Save panel details</button>
      <a class="btn btn-default dirty-check-ignore" href="<@routes.mitcircs.viewPanel panel/>">Cancel</a>
    </div>
  </@f.form>
</#escape>