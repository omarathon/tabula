<#assign tiles=JspTaglibs["http://tiles.apache.org/tags-tiles"]>
<html>
<head>
<style>
#logo {
  background-color: blue;
}
</style>
</head>
<body>
<div id="logo">
<img src="/courses/static/images/logo.png">
</div>
<h1>Assignment management</h1>
<@tiles.insertAttribute name="body" />
</body>
</html>