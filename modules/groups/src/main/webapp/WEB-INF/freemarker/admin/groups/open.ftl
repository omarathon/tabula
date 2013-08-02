<#escape x as x?html>
	<#assign setState = openGroupSetCommand.setState/>
    <#assign smallGroupSet = openGroupSetCommand.singleSetToOpen/>
    <#if openGroupSetCommand.setState.name == 'open'>
    	<#assign submitAction><@routes.openset smallGroupSet /></#assign>
    	<#assign setState = "Open"/>
    <#else>
    	<#assign submitAction><@routes.closeset smallGroupSet /></#assign>
    	<#assign setState = "Close"/>
    </#if>

    <div class="modal-header">
        <h3>${setState}</h3>
    </div>

    <#-- Have to manually remove the 20px margin from the form (added by default bootstrap form styles)-->
    <@f.form method="post" action="${submitAction}" commandName="openGroupSetCommand" cssClass="form-horizonatal form-tiny" style="margin-bottom:0">

    <div class="modal-body">
        <p>${setState} ${smallGroupSet.name} ${smallGroupSet.format.description} for ${smallGroupSet.module.code} for self sign-up.
        <#if setState == "Open"> Students will be notified via email that they can now sign up for these groups in tabula.</#if>
        </p>
    </div>
    <div class="modal-footer">
    <input class="btn btn-info" type="submit" value="${setState}"> <a class="btn cancel-link" data-dismiss="modal" href="#">Cancel</a>
    </div>
    </@f.form>
</#escape>