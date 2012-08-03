package org.apache.tapestry.extend.template;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * template注解，可以在page类使用，页面模板直接写业务界面内容即可，不需要写
 * 模板标签<template ../>了
 * Template 
 *  
 * @author：chenmx@asiainfo-linkage.com
 * @Jul 16, 2012 11:24:44 AM 
 * @version 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Template {
	String name() default "default";
}
