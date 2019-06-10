<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/mitcircs_components.ftl" as components />
<#escape x as x?html>
  <h1>${panel.name}</h1>
  <div id="profile-modal" class="modal fade profile-subset"></div>
  <section class="mitcircs-details">
    <div class="row">
      <div class="col-sm-6 col-md-7">
        <@components.panel_details panel />
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