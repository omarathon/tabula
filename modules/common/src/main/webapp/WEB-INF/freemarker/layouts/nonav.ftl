<#assign tiles=JspTaglibs["http://tiles.apache.org/tags-tiles"]>
<!doctype html>
<html class="iframe" lang="en-GB">
	<head>
		<#include "_head.ftl" />
	</head>
	<body class="horizontal-nav layout-100 tabula-page ${component.bodyClass?default('component-page') ${bodyClasses?default('')}">
		<@tiles.insertAttribute name="body" />
	</body>
</html>