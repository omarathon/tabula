<#escape x as x?html>
<h1>Open groups in ${department.name}</h1>
<#if info.requestParameters.batchOpenSuccess??>
<div class="alert alert-success">
    Students have been notified that these groups are now open for self-sign-up
</div>
</#if>
    <@f.form method="post" action="" commandName="setList" cssClass="form-horizonatal form-tiny">
    <p> Open these groups for self sign-up. Students will be notified via email tha tthey can now sign up for these groups in Tabula
    </p>
        <div class="control-group">
            <input class="btn btn-info" type="submit" value="Open">
        </div>

    <div id="scroll-container">
    <table id="groups-table" class="table table-bordered table-striped sortable-table">

        <thead>
        <tr>
            <th><@form.selector_check_all /></th>
            <th class="sortable">Module/Group</th>
        </tr>
        </thead>
        <tbody >
            <#list groupSets as set>
            <tr ${set.openForSignups?string("class='use-tooltip' title='This group is already open for sign-ups'","")} >
                <td>
                   <@f.checkbox
                   class=set.openForSignups?string('','collection-checkbox')
                   path="checkedGroupsets"
                   disabled=(set.openForSignups)?string
                   value=set.id/>
                </td>
                <td>
                    <span class="${set.openForSignups?string('muted','')}">
                        ${set.module.code} - ${set.module.name} - ${set.name}
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
    jQuery("#groups-table").bigList({});
</script>