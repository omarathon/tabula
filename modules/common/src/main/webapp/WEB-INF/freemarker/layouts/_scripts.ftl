<#-- Scripts that should be included both in the app and embedded in to Sitebuilder -->
<#if info?? && info.requestedUri?? && info.requestedUri.getQueryParameter("debug")??>
	<@script "/static/js/id6scripts.js" />
	<@script "/static/libs/jquery-ui/js/jquery-ui-1.8.16.custom.min.js" />
	<@script "/static/libs/jquery.delayedObserver.js" />
	<@script "/static/libs/jquery-rating/jquery.rating.pack.js" />
	<@script "/static/libs/jquery-caret/jquery.caret.1.02.min.js" />
	<@script "/static/libs/anytime/anytimec.js" />
	<@script "/static/libs/popup/popup.js" />
	<@script "/static/libs/bootstrap/js/bootstrap.js" />
	<@script "/static/libs/bootstrap-editable/js/bootstrap-editable.js" />
	<@script "/static/libs/spin-js/spin.min.js" />
	<@script "/static/libs/spin-js/jquery.spin.js" />
	<@script "/static/js/modernizr.js" />
	<@script "/static/js/browser-info.js" />
<#else>
	<@script "/static/js/render.js" />
	
	<#-- TODO work out why this won't go in concat -->
	<@script "/static/libs/jquery-caret/jquery.caret.1.02.min.js" />
</#if>