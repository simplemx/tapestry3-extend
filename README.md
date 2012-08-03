# Tapestry3.04 扩展

## 1.模板

使用Tapestry的页面会出现很多的html标签，重复在每个页面内，对于很多页面来说，公共部分都是相同的(就算抽取组件之后还是很多冗余)，而只有具体业务部分才不同，鉴于ctrl+c/ctrl+v的大量存在，以及后期维护的压力，这里增加一个模板机制。

业务页面内容通过第一行引入一个标签<template name="**"/>来指定引入的模板，例子如下：

	<template name="common"/>
	<!-- 页面实际内容 -->

而在配置文件里配置common对应的模板url：

	<template name="common" ref="/template/Template.html"/>

实际模板内容为：
	
	<!-- 公共部分-->
	<content/>
	<!-- 公共部分-->

此时在content标签会替代为业务页面内的实际内容。

这样可以使到维护页面公共部分等只需要维护对应的1个模板即可。

而且模板指定还可以通过url传入来进行指定template=common。
