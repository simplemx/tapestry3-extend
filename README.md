# Tapestry3.04 ��չ

## 1.template

ʹ��Tapestry��ҳ�����ֺܶ��html��ǩ���ظ���ÿ��ҳ���ڣ����ںܶ�ҳ����˵���������ֶ�����ͬ��(�����ȡ���֮���Ǻܶ�����)����ֻ�о���ҵ�񲿷ֲŲ�ͬ������ctrl+c/ctrl+v�Ĵ������ڣ��Լ�����ά����ѹ������������һ��ģ����ơ�

ҵ��ҳ������ͨ����һ������һ����ǩ<template name="**"/>��ָ�������ģ�壬�������£�

	<template name="common"/>
	<!-- ҳ��ʵ������ -->

���������ļ�������common��Ӧ��ģ��url��

	<template name="common" ref="/template/Template.html"/>

ʵ��ģ������Ϊ��
	
	<!-- ��������-->
	<content/>
	<!-- ��������-->

��ʱ��content��ǩ�����Ϊҵ��ҳ���ڵ�ʵ�����ݡ�

��������ʹ��ά��ҳ�湫�����ֵ�ֻ��Ҫά����Ӧ��1��ģ�弴�ɡ�

����ģ��ָ��������ͨ��url����������ָ��template=common��

## 2.markup

ʹ��Tapestry��ҳ����Ҫд�ܶ�ı�ǩ�������һ��������ʾ��ǩȴҪд������<span jwcid="@Insert" value="ognl:**"/>�������������JSP�������ںܶ��ģ�����棬Tapestry����㶼�Ǻ��úܿӵ��ġ�

���ԣ�����һ��������̣�ΪTapestry��Insert��Conditional��ǩ���Ͻ��͡�����̵�markup�����<span jwcid="@Insert" value="ognl:**"/>������Tapestry��ǩ�����㿪����ʱ���дģ�塣

����markup��

	${expression}

���ת���ɵ�markup:
	
	<span jwcid="@Insert" value="ognl:expression"/>

����markup��
	
	${#expression}
	${#/}

ת���ɵ�markup��
	
	<span jwcid="@Conditional" condition="ognl:expression">
	</span>