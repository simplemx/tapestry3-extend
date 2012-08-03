//  Copyright 2004 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.apache.tapestry.engine;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tapestry.ApplicationRuntimeException;
import org.apache.tapestry.IAsset;
import org.apache.tapestry.IComponent;
import org.apache.tapestry.IEngine;
import org.apache.tapestry.IMarkupWriter;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.IResourceLocation;
import org.apache.tapestry.Tapestry;
import org.apache.tapestry.extend.tag.TagConverter;
import org.apache.tapestry.extend.template.Template;
import org.apache.tapestry.extend.template.TemplateConfig;
import org.apache.tapestry.parse.ComponentTemplate;
import org.apache.tapestry.parse.ITemplateParserDelegate;
import org.apache.tapestry.parse.TemplateParseException;
import org.apache.tapestry.parse.TemplateParser;
import org.apache.tapestry.parse.TemplateToken;
import org.apache.tapestry.resource.ContextResourceLocation;
import org.apache.tapestry.spec.IApplicationSpecification;
import org.apache.tapestry.spec.IComponentSpecification;
import org.apache.tapestry.util.DelegatingPropertySource;
import org.apache.tapestry.util.IRenderDescription;
import org.apache.tapestry.util.LocalizedPropertySource;
import org.apache.tapestry.util.MultiKey;
import org.apache.tapestry.util.PropertyHolderPropertySource;

/**
 *  Default implementation of {@link ITemplateSource}.  Templates, once parsed,
 *  stay in memory until explicitly cleared.
 *
 *  <p>An instance of this class acts as a singleton shared by all sessions, so it
 *  must be threadsafe.
 *
 *  @author Howard Lewis Ship
 *  @version $Id: DefaultTemplateSource.java 243931 2004-07-22 14:19:37Z hlship $
 * 
 **/

public class DefaultTemplateSource implements ITemplateSource, IRenderDescription
{
    private static final Log LOG = LogFactory.getLog(DefaultTemplateSource.class);


    // The name of the component/application/etc property that will be used to
    // determine the encoding to use when loading the template
         
    private static final String TEMPLATE_ENCODING_PROPERTY_NAME = "org.apache.tapestry.template-encoding"; 

    // Cache of previously retrieved templates.  Key is a multi-key of 
    // specification resource path and locale (local may be null), value
    // is the ComponentTemplate.

    private Map _cache = Collections.synchronizedMap(new HashMap());

    // Previously read templates; key is the IResourceLocation, value
    // is the ComponentTemplate.

    private Map _templates = Collections.synchronizedMap(new HashMap());

    /**
     *  Number of tokens (each template contains multiple tokens).
     *
     **/

    private int _tokenCount;

    private static final int BUFFER_SIZE = 2000;

    private TemplateParser _parser;

    /** @since 2.2 **/

    private IResourceLocation _applicationRootLocation;

    /** @since 3.0 **/

    private ITemplateSourceDelegate _delegate;

    /**
     *  Clears the template cache.  This is used during debugging.
     *
     **/

    public void reset()
    {
        _cache.clear();
        _templates.clear();

        _tokenCount = 0;
    }

    /**
     *  Reads the template for the component.
     *
     *  <p>Returns null if the template can't be found.
     * 
     **/

    public ComponentTemplate getTemplate(IRequestCycle cycle, IComponent component)
    {
        IComponentSpecification specification = component.getSpecification();
        IResourceLocation specificationLocation = specification.getSpecificationLocation();

        Locale locale = component.getPage().getLocale();

        Object key = new MultiKey(new Object[] { specificationLocation, locale }, false);

        ComponentTemplate result = searchCache(key);
        if (result != null)
            return result;

        result = findTemplate(cycle, specificationLocation, component, locale);

        if (result == null)
        {
            result = getTemplateFromDelegate(cycle, component, locale);

            if (result != null)
                return result;

            String stringKey =
                component.getSpecification().isPageSpecification()
                    ? "DefaultTemplateSource.no-template-for-page"
                    : "DefaultTemplateSource.no-template-for-component";

            throw new ApplicationRuntimeException(
                Tapestry.format(stringKey, component.getExtendedId(), locale),
                component,
                component.getLocation(),
                null);
        }

        saveToCache(key, result);

        return result;
    }

    private ComponentTemplate searchCache(Object key)
    {
        return (ComponentTemplate) _cache.get(key);
    }

    private void saveToCache(Object key, ComponentTemplate template)
    {
        _cache.put(key, template);

    }

    private ComponentTemplate getTemplateFromDelegate(
        IRequestCycle cycle,
        IComponent component,
        Locale locale)
    {
        if (_delegate == null)
        {
            IEngine engine = cycle.getEngine();
            IApplicationSpecification spec = engine.getSpecification();

            if (spec.checkExtension(Tapestry.TEMPLATE_SOURCE_DELEGATE_EXTENSION_NAME))
                _delegate =
                    (ITemplateSourceDelegate) spec.getExtension(
                        Tapestry.TEMPLATE_SOURCE_DELEGATE_EXTENSION_NAME,
                        ITemplateSourceDelegate.class);
            else
                _delegate = NullTemplateSourceDelegate.getSharedInstance();

        }

        return _delegate.findTemplate(cycle, component, locale);
    }

    /**
     *  Finds the template for the given component, using the following rules:
     *  <ul>
     *  <li>If the component has a $template asset, use that
     *  <li>Look for a template in the same folder as the component
     *  <li>If a page in the application namespace, search in the application root
     *  <li>Fail!
     *  </ul>
     * 
     *  @return the template, or null if not found
     * 
     **/

    private ComponentTemplate findTemplate(
        IRequestCycle cycle,
        IResourceLocation location,
        IComponent component,
        Locale locale)
    {
        IAsset templateAsset = component.getAsset(TEMPLATE_ASSET_NAME);

        if (templateAsset != null)
            return readTemplateFromAsset(cycle, component, templateAsset);

        String name = location.getName();
        int dotx = name.lastIndexOf('.');
        String templateBaseName = name.substring(0, dotx + 1) + getTemplateExtension(component);

        ComponentTemplate result =
            findStandardTemplate(cycle, location, component, templateBaseName, locale);

        if (result == null
            && component.getSpecification().isPageSpecification()
            && component.getNamespace().isApplicationNamespace())
            result = findPageTemplateInApplicationRoot(cycle, component, templateBaseName, locale);

        return result;
    }

    private ComponentTemplate findPageTemplateInApplicationRoot(
        IRequestCycle cycle,
        IComponent component,
        String templateBaseName,
        Locale locale)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("Checking for " + templateBaseName + " in application root");

        if (_applicationRootLocation == null)
            _applicationRootLocation = Tapestry.getApplicationRootLocation(cycle);

        IResourceLocation baseLocation =
            _applicationRootLocation.getRelativeLocation(templateBaseName);
        IResourceLocation localizedLocation = baseLocation.getLocalization(locale);

        if (localizedLocation == null)
            return null;

        return getOrParseTemplate(cycle, localizedLocation, component);
    }

    /**
     *  Reads an asset to get the template.
     * 
     **/

    private ComponentTemplate readTemplateFromAsset(
        IRequestCycle cycle,
        IComponent component,
        IAsset asset)
    {
        InputStream stream = asset.getResourceAsStream(cycle);

        char[] templateData = null;

        try
        {
            String encoding = getTemplateEncoding(cycle, component, null);
            
            templateData = readTemplateStream(stream, encoding);

            stream.close();
        }
        catch (IOException ex)
        {
            throw new ApplicationRuntimeException(
                Tapestry.format("DefaultTemplateSource.unable-to-read-template", asset),
                ex);
        }

        IResourceLocation resourceLocation = asset.getResourceLocation();

        return constructTemplateInstance(cycle, templateData, resourceLocation, component);
    }

    /**
     *  Search for the template corresponding to the resource and the locale.
     *  This may be in the template map already, or may involve reading and
     *  parsing the template.
     *
     *  @return the template, or null if not found.
     * 
     **/

    private ComponentTemplate findStandardTemplate(
        IRequestCycle cycle,
        IResourceLocation location,
        IComponent component,
        String templateBaseName,
        Locale locale)
    {
        if (LOG.isDebugEnabled())
            LOG.debug(
                "Searching for localized version of template for "
                    + location
                    + " in locale "
                    + locale.getDisplayName());

        IResourceLocation baseTemplateLocation = location.getRelativeLocation(templateBaseName);

        IResourceLocation localizedTemplateLocation = baseTemplateLocation.getLocalization(locale);

        if (localizedTemplateLocation == null)
            return null;

        return getOrParseTemplate(cycle, localizedTemplateLocation, component);

    }

    /**
     *  Returns a previously parsed template at the specified location (which must already
     *  be localized).  If not already in the template Map, then the
     *  location is parsed and stored into the templates Map, then returned.
     * 
     **/

    private ComponentTemplate getOrParseTemplate(
        IRequestCycle cycle,
        IResourceLocation location,
        IComponent component)
    {

        ComponentTemplate result = (ComponentTemplate) _templates.get(location);
        if (result != null)
            return result;

        // Ok, see if it exists.

        result = parseTemplate(cycle, location, component);

        if (result != null)
            _templates.put(location, result);

        return result;
    }

    /**
     *  Reads the template for the given resource; returns null if the
     *  resource doesn't exist.  Note that this method is only invoked
     *  from a synchronized block, so there shouldn't be threading
     *  issues here.
     *
     **/

    private ComponentTemplate parseTemplate(
        IRequestCycle cycle,
        IResourceLocation location,
        IComponent component)
    {
        String encoding = getTemplateEncoding(cycle, component, location.getLocale());
        
        //test begin
        //origin code
//        char[] templateData = readTemplate(location, encoding);
        //new code
        //TODO 为模板加上缓存
        
        char[] templateData = TagConverter.enhenceTag(readTemplate(location, encoding));
        
        //如果为page，那么判断是否有模板，有的话查找模板并且将数据进行合并
        
        //如何配置
        //1.在page.html内最开始的地方加上<template url="/Template.html" name="ueui"/>
        //表明使用的模板为Template.html.如果没有url属性就以name从templateconfig中查找对应的url
        //或者在page.java类前面加上注解@Template(name="ueui")来进行查找
        //2.模板 Template.html内加上<content/>，表明将会在这里加入page.html内的内容
        if(component.getSpecification().isPageSpecification()){
        	try{
        		String templateName = cycle.getRequestContext().getRequest()
        			.getParameter(TemplateConfig.TEMPLATE_PARAM);
	        	
        		// 查找模板对应的url
				TemplateFinder finder = new TemplateFinder(templateData);
				String templateUrl = null;
        		if(StringUtils.isNotEmpty(templateName)){
        			templateUrl = TemplateConfig.getInstance().getUrlByName(templateName);
        		} 
        		else {
					// 从html模板中查找对应的模板url，如果html要求有模板，可以在html最开开始
					// 的时候加上<template url=""/>
					templateUrl = finder.findTemplateUrl();
					if (StringUtils.isNotEmpty(templateUrl)) {
						templateData = finder.getContentData();
					} 
					else {
						templateUrl = findTemplateUrlByAnnotation(component);
					}
        		}

				// 处理模板内仍然包含模板
				char[] pageTemplateData = null;
				List<char[]> stack = new ArrayList<char[]>();
				while (StringUtils.isNotBlank(templateUrl)) {
					pageTemplateData = readTemplate(getTemplateLocation(cycle,
							templateUrl), encoding);
					finder.reset(pageTemplateData);
					templateUrl = finder.findTemplateUrl();
					pageTemplateData = finder.getContentData();
					stack.add(pageTemplateData);
				}
	        	for(char[] each:stack){
	        		templateData = mergeTemplate(templateData, each);
	        	}
        	} catch (Exception e) {
				e.printStackTrace();
				if (LOG.isDebugEnabled()) {
					LOG.debug("查找模板失败，原因为" + e.getMessage());
				}
			}
        }
        // test end
        
        if (templateData == null)
            return null;

        return constructTemplateInstance(cycle, templateData, location, component);
    }
    
    //test begin
    /**
     * 根据读入的char串获取对应模板url的查找类
     * 查找逻辑全部封装在此
     * <template name="" url=""/>如果有输入url以url为主，如果没有那么
     * 根据name从templateconfig中获取对应的url，如果既没有name也没有url
     * 那么按照默认的default传入到templateconfig中获取url
     */
    static class TemplateFinder {
    	private static final char[] TEMPLATE_TAG = new char[] { '<', 't', 'e',
    		'm', 'p', 'l', 'a', 't', 'e' };
        private static final char[] URL_ATTR_PREFIX = new char[] { 'u', 'r', 'l',
    			'=' };
        private static final char[] NAME_ATTR_PREFIX = new char[] { 'n', 'a', 'm',
				'e', '=' }; 
        private static final char[] TEMPLAE_END_TAG = new char[] { '/', '>' };
    	
    	char[] pageData = null;
    	int cursor = -1;
    	
    	TemplateFinder(char[] pageData){
    		this.pageData = pageData;
    	}
    	
    	public void reset(char[] pageData){
    		this.cursor = -1;
    		this.pageData = pageData;
    	}
    	
    	public String findTemplateUrl() {
			if (pageData == null || pageData.length <= TEMPLATE_TAG.length) {
				return null;
			}

			cursor = 0;
			String url = null;
			try {
				// skip not tag stuff in beginning
				while (pageData[cursor] != '<') {
					cursor++;
				}
				
				//skip <template
				for (int i = 0; i < TEMPLATE_TAG.length; i++) {
					if (pageData[cursor++] != TEMPLATE_TAG[i]) {
						return null;
					}
				}
				
				//get the name attr or url attr
				skipWhiteSpace();
				String name = null;
				while (!isPatternFound(cursor, TEMPLAE_END_TAG)) {
					if (isPatternFound(cursor, URL_ATTR_PREFIX)) {
						cursor += URL_ATTR_PREFIX.length;
						url = findValue();
						if (url == null) {
							return null;
						}
						cursor++;
					} 
					else if (isPatternFound(cursor, NAME_ATTR_PREFIX)) {
						cursor += NAME_ATTR_PREFIX.length;
						name = findValue();
						if (name == null) {
							return null;
						}
						cursor++;
					}
					skipWhiteSpace();
				}

				if (StringUtils.isEmpty(url)) {
					if (StringUtils.isNotEmpty(name)) {
						url = TemplateConfig.getInstance().getUrlByName(name);
					} 
					else {
						url = TemplateConfig.getInstance().getUrlByName(
								TemplateConfig.DEFAULT);
					}
				}
			} catch (IndexOutOfBoundsException e) {
				
			} finally {
				if(url == null){
					cursor = -1;
				}
			}
			return url;
		}
    	
    	private String findValue() {
			skipWhiteSpace();
			// skip quote
			if (isNotQuotePresent()) {
				return null;
			}
			cursor++;
			// find the end of url value
			int from = cursor;
			int to = cursor;
			while (isNotQuotePresent()) {
				cursor++;
			}
			to = cursor;
			return new String(Arrays.copyOfRange(pageData, from, to));
		}
    	
    	private boolean isPatternFound(int cursor, char[] pattern) {
			for (int i = 0; i < pattern.length && cursor < pageData.length; i++) {
				if (pageData[cursor++] != pattern[i]) {
					return false;
				}
			}
			return true;
		}
    	
    	private boolean isNotQuotePresent(){
    		return pageData[cursor] != '\'' && pageData[cursor] != '"';
    	}
    	
    	private void skipWhiteSpace() throws IndexOutOfBoundsException{
    		while (pageData[cursor] == ' ') {
				cursor++;
			}
    	}
    	
    	//return the new content of remove <template url=''/>
    	public char[] getContentData(){
    		//cursor point to the position of ' or "
    		if(this.cursor > 0){
    			try {
					cursor++;

					//find the next > and all the content is from the position 
					//after the > to the end of the pageData
					while(pageData[cursor++] != '>');
						
					return Arrays.copyOfRange(pageData, cursor, pageData.length);
				} catch (Exception e) {
				
				}
    		}
    		return this.pageData;
    	}
    }
    
    //从注解中查找模板路径
    protected String findTemplateUrlByAnnotation(IComponent component) {
		Class clazz = component.getClass();
		while (clazz != null && !clazz.isAnnotationPresent(Template.class)) {
			clazz = clazz.getSuperclass();
		}
		String url = null;
		if (clazz != null && clazz.isAnnotationPresent(Template.class)) {
			Template annotation = (Template) clazz.getAnnotation(Template.class);
			if (StringUtils.isNotEmpty(annotation.name())) {
				url = TemplateConfig.getInstance().getUrlByName(annotation.name());
			}
		}

		return url;
	}
    
    private IResourceLocation getTemplateLocation(IRequestCycle cycle,
			String path) {
		return new ContextResourceLocation(cycle.getRequestContext()
				.getServlet().getServletContext(), path);
	}
    
    protected static final char[] CONTENT_TAG = new char[] { '<', 'c', 'o',
			'n', 't', 'e', 'n', 't', '/', '>' };

	private boolean isContentTagFound(char[] match, char[] templateData,
			int cursor) {
		try {
			for (int i = 0; i < match.length; i++) {
				if (templateData[cursor + i] != match[i]) {
					return false;
				}
			}
			return true;
		} catch (IndexOutOfBoundsException ex) {
			return false;
		}
	}
    
	//将模板数据和页面内容合并
    protected char[] mergeTemplate(char[] pageData, char[] templateData) {
		int cursor = 0;
		int length = templateData.length;
		while (cursor < length) {
			if (isContentTagFound(CONTENT_TAG, templateData, cursor)) {
				// 如果找到将当前的content tag去掉，替换为pageData的内容
				char[] before = Arrays.copyOfRange(templateData, 0, cursor);
				char[] after = Arrays.copyOfRange(templateData, cursor
						+ CONTENT_TAG.length, length);
				char[] newTemplateData = new char[before.length + after.length
						+ pageData.length];
				int index = 0;
				for (int i = 0; i < before.length; i++) {
					newTemplateData[index++] = before[i];
				}
				for (int i = 0; i < pageData.length; i++) {
					newTemplateData[index++] = pageData[i];
				}
				for (int i = 0; i < after.length; i++) {
					newTemplateData[index++] = after[i];
				}
				return newTemplateData;
			}
			cursor++;
		}
		return pageData;
	}
    // test end
    
    /**
	 * This method is currently synchronized, because {@link TemplateParser} is
	 * not threadsafe. Another good candidate for a pooling mechanism,
	 * especially because parsing a template may take a while.
	 * 
	 */

    private synchronized ComponentTemplate constructTemplateInstance(
        IRequestCycle cycle,
        char[] templateData,
        IResourceLocation location,
        IComponent component)
    {
        if (_parser == null)
            _parser = new TemplateParser();

        ITemplateParserDelegate delegate = new TemplateParserDelegateImpl(component, cycle);

        TemplateToken[] tokens;

        try
        {
            tokens = _parser.parse(templateData, delegate, location);
        }
        catch (TemplateParseException ex)
        {
            throw new ApplicationRuntimeException(
                Tapestry.format("DefaultTemplateSource.unable-to-parse-template", location),
                ex);
        }

        if (LOG.isDebugEnabled())
            LOG.debug("Parsed " + tokens.length + " tokens from template");

        _tokenCount += tokens.length;

        return new ComponentTemplate(templateData, tokens);
    }

    /**
     *  Reads the template, given the complete path to the
     *  resource.  Returns null if the resource doesn't exist.
     *
     **/

    private char[] readTemplate(IResourceLocation location, String encoding)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("Reading template " + location);

        URL url = location.getResourceURL();

        if (url == null)
        {
            if (LOG.isDebugEnabled())
                LOG.debug("Template does not exist.");

            return null;
        }

        if (LOG.isDebugEnabled())
            LOG.debug("Reading template from URL " + url);

        InputStream stream = null;

        try
        {
            stream = url.openStream();

            return readTemplateStream(stream, encoding);
        }
        catch (IOException ex)
        {
            throw new ApplicationRuntimeException(
                Tapestry.format("DefaultTemplateSource.unable-to-read-template", location),
                ex);
        }
        finally
        {
            Tapestry.close(stream);
        }

    }

    /**
     *  Reads a Stream into memory as an array of characters.
     *
     **/

    private char[] readTemplateStream(InputStream stream, String encoding) throws IOException
    {
        char[] charBuffer = new char[BUFFER_SIZE];
        StringBuffer buffer = new StringBuffer();

        InputStreamReader reader;
        if (encoding != null)
            reader = new InputStreamReader(new BufferedInputStream(stream), encoding);
        else
            reader = new InputStreamReader(new BufferedInputStream(stream));

        try
        {
            while (true)
            {
                int charsRead = reader.read(charBuffer, 0, BUFFER_SIZE);

                if (charsRead <= 0)
                    break;

                buffer.append(charBuffer, 0, charsRead);
            }
        }
        finally
        {
            reader.close();
        }

        // OK, now reuse the charBuffer variable to
        // produce the final result.

        int length = buffer.length();

        charBuffer = new char[length];

        // Copy the character out of the StringBuffer and into the
        // array.

        buffer.getChars(0, length, charBuffer, 0);

        return charBuffer;
    }

    public String toString()
    {
        ToStringBuilder builder = new ToStringBuilder(this);

        builder.append("tokenCount", _tokenCount);

        builder.append("templates", _templates.keySet());

        return builder.toString();
    }

    /**
     *  Checks for the {@link Tapestry#TEMPLATE_EXTENSION_PROPERTY} in the component's
     *  specification, then in the component's namespace's specification.  Returns
     *  {@link Tapestry#DEFAULT_TEMPLATE_EXTENSION} if not otherwise overriden.
     * 
     **/

    private String getTemplateExtension(IComponent component)
    {
        String extension =
            component.getSpecification().getProperty(Tapestry.TEMPLATE_EXTENSION_PROPERTY);

        if (extension != null)
            return extension;

        extension =
            component.getNamespace().getSpecification().getProperty(
                Tapestry.TEMPLATE_EXTENSION_PROPERTY);

        if (extension != null)
            return extension;

        return Tapestry.DEFAULT_TEMPLATE_EXTENSION;
    }

    /** @since 1.0.6 **/

    public synchronized void renderDescription(IMarkupWriter writer)
    {
        writer.print("DefaultTemplateSource[");

        if (_tokenCount > 0)
        {
            writer.print(_tokenCount);
            writer.print(" tokens");
        }

        if (_cache != null)
        {
            boolean first = true;
            Iterator i = _cache.entrySet().iterator();

            while (i.hasNext())
            {
                if (first)
                {
                    writer.begin("ul");
                    first = false;
                }

                Map.Entry e = (Map.Entry) i.next();
                Object key = e.getKey();
                ComponentTemplate template = (ComponentTemplate) e.getValue();

                writer.begin("li");
                writer.print(key.toString());
                writer.print(" (");
                writer.print(template.getTokenCount());
                writer.print(" tokens)");
                writer.println();
                writer.end();
            }

            if (!first)
            {
                writer.end(); // <ul>
                writer.beginEmpty("br");
            }
        }

        writer.print("]");

    }
    
    private String getTemplateEncoding(IRequestCycle cycle, IComponent component, Locale locale)
    {
        IPropertySource source = getComponentPropertySource(cycle, component);

        if (locale != null)
            source = new LocalizedPropertySource(locale, source);

        return getTemplateEncodingProperty(source);
    }
    
    private IPropertySource getComponentPropertySource(IRequestCycle cycle, IComponent component)
    {
        DelegatingPropertySource source = new DelegatingPropertySource();

        // Search for the encoding property in the following order:
        // First search the component specification
        source.addSource(new PropertyHolderPropertySource(component.getSpecification()));

        // Then search its library specification
        source.addSource(new PropertyHolderPropertySource(component.getNamespace().getSpecification()));

        // Then search the rest of the standard path
        source.addSource(cycle.getEngine().getPropertySource());
        
        return source;
    }
    
    private String getTemplateEncodingProperty(IPropertySource source)
    {
        return source.getPropertyValue(TEMPLATE_ENCODING_PROPERTY_NAME);
    }
    
}
