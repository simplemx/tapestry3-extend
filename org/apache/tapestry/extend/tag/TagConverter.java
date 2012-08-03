package org.apache.tapestry.extend.tag;

import java.util.Arrays;

public class TagConverter {
	
	private static final char[] INSERT_BEGIN = {'$','{'};
	
	private static final String INSERT_TAG_BEGIN = "<span jwcid=\"@Insert\" value=\"ognl:";
	private static final String INSERT_TAG_END = "\"/>";
	
	/**
	 * 将${expression} 变成 <span jwcid="@Insert" value="ognl:expression"/>
	 * 
	 *
	 * @param templateData
	 * @return char[]
	 * @Exception
	 */
	public static char[] enhenceTag(char[] templateData ){
		for(int i = 0 ; i < templateData.length ; i++){
			if(encounterTag(templateData, i, INSERT_BEGIN)){
				int begin = i;
				while(templateData[i] != '}' && i < templateData.length){
					i++;
				}
				if (i < templateData.length) {
					char[] expression = Arrays.copyOfRange(templateData,
							begin + 2, i);
					char[] before = Arrays.copyOfRange(templateData, 0, begin);
					char[] after = Arrays.copyOfRange(templateData, i + 1,
							templateData.length);
					char[] newTemplateData = new char[expression.length
							+ before.length + after.length
							+ INSERT_TAG_BEGIN.length()
							+ INSERT_TAG_END.length()];
					int index = 0;
					for (int j = 0; j < before.length; j++) {
						newTemplateData[index++] = before[j];
					}
					for (int j = 0; j < INSERT_TAG_BEGIN.length(); j++) {
						newTemplateData[index++] = INSERT_TAG_BEGIN.charAt(j);
					}
					for (int j = 0; j < expression.length; j++) {
						newTemplateData[index++] = expression[j];
					}
					for (int j = 0; j < INSERT_TAG_END.length(); j++) {
						newTemplateData[index++] = INSERT_TAG_END.charAt(j);
					}
					for (int j = 0; j < after.length; j++) {
						newTemplateData[index++] = after[j];
					}

					templateData = newTemplateData;
					i = begin + INSERT_TAG_BEGIN.length() + expression.length
							+ INSERT_TAG_END.length();
				}
			}
		}
		return templateData;
	}
	
	private static boolean encounterTag(char[] templateData, int cursor,char[] tag){
		int tagIndex = 0;
		while(templateData[cursor] == tag[tagIndex] ){
			if(tagIndex == tag.length - 1){
				return true;
			}
			tagIndex++;
			cursor++;
		}
		return false;
	}
}
