<div id="feedback-rating">

<#assign command=rateFeedbackCommand/>
<#assign topstars=command.maximumStars/><#--

Interface for students to leave a rating about the feedback they've received.

-->
<h3>Rate your feedback</h3>

<p>
Rating feedback is anonymous and will help us to give you better feedback
in future. Please try to rate solely on the quality of the feedback.
</p>
<#if command.effectiveRating??>
<#assign rating=command.effectiveRating />
</#if>

<#if rating??>
<#-- Has a rating -->
<!-- Current rating ${rating} -->
<#else>
<#-- No rating set yet -->
<!-- No current rating -->
</#if>

<form id="feedback-rating-form" method="POST" action="<@routes.ratefeedback command.feedback />">

	<noscript>1</noscript>
	<#list 1..topstars as stars>
	    <input type="radio" name="rating" value="${stars}" <#if rating?? && rating = stars>checked</#if> />
	</#list>
	
	<noscript>${topstars}
	<div><input type="checkbox" name="unset"> withdraw your rating</div>
	<input type="submit" value="Rate" />
	</noscript>

</form>
<div class="end-floats"></div>
<#if rated?? && command.unset>
<p class="subtle">Your rating has been removed.</p>
<#elseif rating??>
<p class="subtle">Thanks for rating.</p>
</#if>

</div>