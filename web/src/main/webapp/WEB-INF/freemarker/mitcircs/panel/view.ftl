<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/mitcircs_components.ftl" as components />
<#import "/WEB-INF/freemarker/modal_macros.ftlh" as modal />
<#escape x as x?html>
  <h1>${panel.name}</h1>
  <@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>
  <section class="mitcircs-details">
    <div class="row">
      <div class="col-sm-6 col-md-7">
        <@components.panelDetails panel />
      </div>
      <div class="col-sm-6 col-md-4">
        <div class="row form-horizontal">
          <div class="col-sm-4 control-label">Actions</div>
          <div class="col-sm-8">
            <#-- If the user has permission to admin the whole department, return them to the whole-department list -->
            <#if can.do("MitigatingCircumstancesSubmission.Read", panel.department)>
              <p><a href="<@routes.mitcircs.listPanels panel.department panel.academicYear />" class="btn btn-default btn-block"><i class="fal fa-long-arrow-left"></i> Return to list of panels</a></p>
            <#else>
              <p><a href="<@routes.mitcircs.home />" class="btn btn-default btn-block"><i class="fal fa-long-arrow-left"></i> Return to list of panels</a></p>
            </#if>

            <#if can.do("MitigatingCircumstancesPanel.Modify", panel)>
              <p><a href="<@routes.mitcircs.editPanel panel />" class="btn btn-default btn-block">Edit panel</a></p>
            </#if>
          </div>
        </div>
      </div>
    </div>
    <@components.section  label="Submissions">
      <@components.submissionTable submissions=(panel.submissions![])?sort_by('key') actions=false panel=false forPanel=true submissionStages=submissionStages />
    </@components.section>
  </section>
</#escape>