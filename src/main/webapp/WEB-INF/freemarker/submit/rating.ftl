<#--

Interface for students to leave a rating about the feedback they've received.

-->
<h3>Rate your feedback</h3>

<p>
Rating feedback is anonymous and will help us to give you better feedback
in future. Please try to rate solely on the quality of the feedback, ignoring
and mark you may have got.
</p>

<#if command.rating??>
<#-- Has a rating -->

<#else>
<#-- No rating set yet -->

</#if>
<#assign topstars=5/>
<form id="feedback-rating-form" method="POST" action="<@routes.ratefeedback command.feedback />">
1
<#list 1..topstars as stars>
    <input type="radio" name="rating" value="${stars}" <#if command.rating?? && command.rating = stars>checked</#if> />
</#list>
${topstars}
<input type="submit" value="Rate" >
</form>