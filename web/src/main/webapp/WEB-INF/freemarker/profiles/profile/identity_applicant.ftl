<#escape x as x?html>

  <#if user.staff>
    <#include "search/_form.ftl" />
    <hr class="full-width" />
  </#if>

  <h1>Applicant</h1>

  <div class="row">
    <div class="col-md-6">
      <h2>${member.fullName}</h2>

      <div class="row">
        <div class="col-md-5 col-lg-4">
          <@fmt.member_photo member />
        </div>
        <div class="col-md-7 col-lg-8">
          <strong>Name:</strong> ${member.fullName}<br />

          <br />

          <#if member.email??>
            <strong>Warwick email:</strong> <a href="mailto:${member.email}">${member.email}</a><br />
          </#if>
          <#if member.universityId??>
            <strong>University ID: </strong> ${member.universityId}<br />
          </#if>
          <#if member.userId??>
            <strong>Username:</strong> ${member.userId}<br />
          </#if>
          <#if member.dateOfBirth??>
            <strong>Date of birth:</strong> <@fmt.date date=member.dateOfBirth includeTime=false relative=false shortMonth=true /><br />
          </#if>
          <#if member.homeEmail??>
            <strong>Alternative email:</strong> ${member.homeEmail}<br />
          </#if>
          <#if member.mobileNumber??>
            <strong>Mobile phone:</strong> ${member.mobileNumber}<br />
          </#if>

          <#if user.sysadmin>
            <span tabindex="0" class="tabula-tooltip" data-title="This information is only available to sysadmins"><i class="fal fa-user-crown"></i></span>
            <#if member.lastImportDate??><strong>Last import:</strong> <@fmt.date date=member.lastImportDate /></#if>
            <@f.form method="post" action="${url('/sysadmin/import-profiles')}" modelAttribute="" style="display: inline;">
              <input type="hidden" name="members" value="${member.universityId}">
              <input class="btn btn-danger btn-xs" type="submit" value="Re-import now">
            </@f.form>
            <br />
          </#if>
        </div>
      </div>

    </div>
    <div class="col-md-6"></div>
  </div>
</#escape>
