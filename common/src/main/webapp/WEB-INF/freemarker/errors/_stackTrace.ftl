<#if exception?? && ((features.renderStackTracesForAllUsers)!false || (user.sysadmin)!false)>
  <p>
    <button type="button" class="btn btn-danger" data-toggle="collapse" data-target="#dev">
      Show technical details about this error
    </button>
  </p>

  <pre id="dev" class="collapse" style="overflow-x:scroll;">${stackTrace}</pre>
</#if>