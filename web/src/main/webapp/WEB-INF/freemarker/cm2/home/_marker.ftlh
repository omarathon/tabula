<#import "*/coursework_components.ftl" as components />

<#if !embedded>
  <h1>Assignments for marking</h1>

  <#if !markerInformation??>
    <div class="marker-information">
      <p class="hint"><i class="id7-koan-spinner id7-koan-spinner--xs id7-koan-spinner--inline" aria-hidden="true"></i> Loading&hellip;</p>
    </div>

    <script type="text/javascript" nonce="${nonce()}">
      (function ($) {
        $('.marker-information').load('<@routes.cm2.markerHome />');
      })(jQuery);
    </script>
  </#if>
</#if>

<#if markerInformation??>
  <@components.marker_assignment_list id="marker-action" title="Action required" assignments=markerInformation.actionRequiredAssignments verb="Mark" show_actions=showMarkingActions marker=marker />
  <@components.marker_assignment_list id="marker-noaction" title="No action required" assignments=markerInformation.noActionRequiredAssignments verb="Review" expand_by_default=(!markerInformation.actionRequiredAssignments?has_content) show_actions=showMarkingActions marker=marker />
  <@components.marker_assignment_list id="marker-upcoming" title="Upcoming" assignments=markerInformation.upcomingAssignments verb="Review" expand_by_default=(!markerInformation.actionRequiredAssignments?has_content && !markerInformation.noActionRequiredAssignments?has_content) show_actions=showMarkingActions marker=marker />
  <@components.marker_assignment_list id="marker-completed" title="Completed" assignments=markerInformation.completedAssignments verb="Review" expand_by_default=(!markerInformation.actionRequiredAssignments?has_content && !markerInformation.noActionRequiredAssignments?has_content && !markerInformation.upcomingAssignments?has_content) show_actions=showMarkingActions marker=marker />

  <script type="text/javascript" nonce="${nonce()}">
    (function ($) {
      $('.use-tooltip').tooltip({ sanitize: false, });
      GlobalScripts.initCollapsible();
    })(jQuery);
  </script>
</#if>
