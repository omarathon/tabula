<#escape x as x?html>
<h1>Notify groups in ${department.name}</h1>
<#if info.requestParameters.batchReleaseSuccess??>
<div class="alert alert-success">
    Small group notifications have been sent
</div>
</#if>
    <@f.form method="post" action="" commandName="moduleList" cssClass="form-horizonatal form-tiny">
    <p> Notify these people via email that these groups are ready to view in Tabula
    </p>
        <@form.row "notifyStudents">
        <label class="checkbox">
            <@f.checkbox path="notifyStudents"/>Students
        </label>
        </@form.row>
        <@form.row "notifyTutors" >
        <label class="checkbox">
            <@f.checkbox path="notifyTutors"/>Tutors
        </label>
        </@form.row>
        <div class="control-group">
            <input class="btn btn-info" type="submit" value="Notify">
        </div>

    <div id="scroll-container">
    <div id="tooltip-container"></div>
    <table id="modules-table" class="table table-bordered table-striped sortable-table">

        <thead>
        <tr>
            <th><@form.selector_check_all /></th>
            <th class="sortable">Module</th>
        </tr>
        </thead>
        <tbody >
            <#list modules as module>
            <tr ${module.hasUnreleasedGroupSets?string("","class='use-tooltip' title='Notifications about groups for this module have already been sent'")} data-container="#tooltip-container">
                <td>
                   <@f.checkbox
                   class=module.hasUnreleasedGroupSets?string('collection-checkbox','')
                   path="checkedModules"
                   disabled=(!module.hasUnreleasedGroupSets)?string
                   value=module.code/>
                </td>
                <td>
                    <span class="${module.hasUnreleasedGroupSets?string('','muted')}">
                        ${module.code} - ${module.name}
                    </span>
                </td>
            </tr>
            </#list>

        </tbody>
    </table>
    </div>


    </@f.form>
</#escape>
<script type="text/javascript" src="/static/libs/jquery-tablesorter/jquery.tablesorter.min.js"></script>
<script type="text/javascript" src="/static/js/sortable-table.js"></script>
<script type="text/javascript">
    jQuery("#modules-table").bigList({});
</script>