package org.apache.tapestry.extend.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.linkage.appframework.common.Common;
import com.linkage.appframework.data.IData;
import com.linkage.appframework.data.IDataset;

/**
 * 模板配置类。
 * 读取模板配置文件templateconfig.xml，读入name以及对应url。
 * 
 * TemplateConfig 
 *  
 * @author：chenmx@asiainfo-linkage.com
 * @Jul 16, 2012 11:28:21 AM 
 * @version 1.0
 */
public class TemplateConfig {
	public static final String DEFAULT = "default";
	public static final String TEMPLATE_PARAM = "template";
	private static Common common = Common.getInstance();
	private static TemplateConfig instance = new TemplateConfig();
	
	private static final String KEY_NAME = "name";
	private static final String KEY_URL = "url";
	private static final String KEY_REF = "ref";
	
	private Map<String, String> hash = new HashMap<String, String>();

	private TemplateConfig() {
		init();
	}
	
	private void init(){
		try {
			IDataset list = common.getElements("templateconfig.xml", "config/template");
			IData config = null;
			List<IData> refList = new ArrayList<IData>(list.size());
			for(int i = 0 ; i < list.size() ; i++){
				config = list.getData(i);
				if(StringUtils.isNotEmpty(config.getString(KEY_URL))){
					hash.put(config.getString(KEY_NAME), config.getString(KEY_URL));
				}
				else {
					refList.add(config);
				}
			}
			for(IData each:refList){
				if(StringUtils.isNotEmpty(each.getString(KEY_REF))){
					hash.put(each.getString(KEY_NAME), hash.get(each.getString(KEY_REF)));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public static TemplateConfig getInstance() {
		return instance;
	}
	
	public String getUrlByName(String name){
		return hash.get(name);
	}
}
