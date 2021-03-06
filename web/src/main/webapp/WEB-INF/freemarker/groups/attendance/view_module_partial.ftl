<#escape x as x?html>
  <#import "*/group_components.ftlh" as components />

  <#if nonempty(sets?keys)>
    <@components.single_module_attendance_contents module sets />

    <script nonce="${nonce()}">
      jQuery('.use-popover').tabulaPopover({
        trigger: 'click focus',
        container: 'body'
      });
    </script>
  <#else>
    <div class="item-info clearfix">
      <p>There are no small group events for <@fmt.module_name module false /></p>
    </div>
  </#if>
</#escape>