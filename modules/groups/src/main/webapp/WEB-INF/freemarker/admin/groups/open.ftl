<#escape x as x?html>
    <#assign smallGroupSet = openGroupSetCommand.singleSetToOpen/>
    <#assign submitAction><@routes.openset smallGroupSet /></#assign>

    <div class="modal-header">
        <h3>Open</h3>
    </div>

    <#-- Have to manually remove the 20px margin from the form (added by default bootstrap form styles)-->
    <@f.form method="post" action="${submitAction}" commandName="openGroupSetCommand" cssClass="form-horizonatal form-tiny" style="margin-bottom:0">

    <div class="modal-body">
        <p>Open ${smallGroupSet.name} ${smallGroupSet.format.description} for ${smallGroupSet.module.code} for self sign-up. Students will
        be notified via email that they can now sign up for these groups in tabula
        </p>
    </div>
    <div class="modal-footer">
    <input class="btn btn-info" type="submit" value="Open"> <a class="btn cancel-link" data-dismiss="modal" href="#">Cancel</a>
    </div>
    </@f.form>
</#escape>