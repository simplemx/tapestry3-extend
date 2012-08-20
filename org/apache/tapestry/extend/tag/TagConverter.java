package org.apache.tapestry.extend.tag;

import java.util.Arrays;

public class TagConverter {

	private static final char[] INSERT_BEGIN = { '$', '{' };

	private static final String INSERT_TAG_BEGIN = "<span jwcid=\"@Insert\" value=\"ognl:";
	private static final String INSERT_TAG_END = "\"/>";

	private static final char[] CONDICTION_BEGIN = { '$', '{', '#' };
	private static final String CONDICTION_TAG_BEGIN = "<span jwcid=\"@Conditional\" condition=\"ognl:";
	private static final String CONDICTION_TAG_END = "\">";

	private static final char[] END_CONDICTION = { '$', '{', '#', '/', '}' };
	private static final String END_TAG = "</span>";

	/**
	 * 1.将${expression} 变成 <span jwcid="@Insert" value="ognl:expression"/>
	 * 2.将${#condiction} 变成<span jwcid="@Conditional"
	 * condition="ognl:condiction"> 将${#/}变成</span>
	 * 
	 * 
	 * @param templateData
	 * @return char[]
	 * @Exception
	 */
	public static char[] enhenceTag(char[] templateData) {
		int begin = 0;
		for (int i = 0; i < templateData.length; i++) {
			if (encounterTag(templateData, i, END_CONDICTION)) {
				begin = i;
				if (i < templateData.length) {
					int afterLength = (templateData.length - i - END_CONDICTION.length);
					char[] newTemplateData = new char[begin + afterLength
							+ END_TAG.length()];
					System.arraycopy(templateData, 0, newTemplateData, 0,
									begin);
					System.arraycopy(END_TAG.toCharArray(), 0, newTemplateData,
							begin, END_TAG.length());
					System.arraycopy(templateData, begin
							+ END_CONDICTION.length, newTemplateData, begin
							+ END_TAG.length(), afterLength);
					templateData = newTemplateData;
					i = begin + END_TAG.length();
				}
			} else if (encounterTag(templateData, i, CONDICTION_BEGIN)) {
				begin = i;
				while (templateData[i] != '}' && i < templateData.length) {
					i++;
				}
				if (i < templateData.length) {
					int expressionLength = i - begin -3;
					int afterLength = templateData.length - i - 1;
					
					char[] newTemplateData = new char[expressionLength
					      							+ begin + afterLength
					      							+ CONDICTION_TAG_BEGIN.length()
					      							+ CONDICTION_TAG_END.length()];
					System.arraycopy(templateData, 0, newTemplateData, 0, begin);
					System.arraycopy(CONDICTION_TAG_BEGIN.toCharArray(), 0, newTemplateData, begin, CONDICTION_TAG_BEGIN.length());
					System.arraycopy(templateData, begin+3, newTemplateData, begin+CONDICTION_TAG_BEGIN.length(), expressionLength);
					System.arraycopy(CONDICTION_TAG_END.toCharArray(), 0, newTemplateData, begin+CONDICTION_TAG_BEGIN.length()+expressionLength, CONDICTION_TAG_END.length());
					System.arraycopy(templateData, i + 1, newTemplateData, begin + CONDICTION_TAG_BEGIN.length() + expressionLength + CONDICTION_TAG_END.length(), afterLength);
					templateData = newTemplateData;
					i = begin + CONDICTION_TAG_BEGIN.length()
							+ expressionLength + CONDICTION_TAG_END.length();
					
//					char[] expression = Arrays.copyOfRange(templateData,
//							begin + 3, i);
//					char[] before = Arrays.copyOfRange(templateData, 0, begin);
//					char[] after = Arrays.copyOfRange(templateData, i + 1,
//							templateData.length);
//					char[] newTemplateData = new char[expression.length
//							+ before.length + after.length
//							+ CONDICTION_TAG_BEGIN.length()
//							+ CONDICTION_TAG_END.length()];
//					int index = 0;
//					for (int j = 0; j < before.length; j++) {
//						newTemplateData[index++] = before[j];
//					}
//					for (int j = 0; j < CONDICTION_TAG_BEGIN.length(); j++) {
//						newTemplateData[index++] = CONDICTION_TAG_BEGIN
//								.charAt(j);
//					}
//					for (int j = 0; j < expression.length; j++) {
//						newTemplateData[index++] = expression[j];
//					}
//					for (int j = 0; j < CONDICTION_TAG_END.length(); j++) {
//						newTemplateData[index++] = CONDICTION_TAG_END.charAt(j);
//					}
//					for (int j = 0; j < after.length; j++) {
//						newTemplateData[index++] = after[j];
//					}
//
//					templateData = newTemplateData;
//					i = begin + CONDICTION_TAG_BEGIN.length()
//							+ expression.length + CONDICTION_TAG_END.length();
				}
			} else if (encounterTag(templateData, i, INSERT_BEGIN)) {
				begin = i;
				while (templateData[i] != '}' && i < templateData.length) {
					i++;
				}
				if (i < templateData.length) {
					int expressionLength = i - begin -2;
					int afterLength = templateData.length - i - 1;
					
					char[] newTemplateData = new char[expressionLength
					      							+ begin + afterLength
					      							+ INSERT_TAG_BEGIN.length()
					      							+ INSERT_TAG_END.length()];
					System.arraycopy(templateData, 0, newTemplateData, 0, begin);
					System.arraycopy(INSERT_TAG_BEGIN.toCharArray(), 0, newTemplateData, begin, INSERT_TAG_BEGIN.length());
					System.arraycopy(templateData, begin+2, newTemplateData, begin+INSERT_TAG_BEGIN.length(), expressionLength);
					System.arraycopy(INSERT_TAG_END.toCharArray(), 0, newTemplateData, begin+INSERT_TAG_BEGIN.length()+expressionLength, INSERT_TAG_END.length());
					System.arraycopy(templateData, i + 1, newTemplateData, begin + INSERT_TAG_BEGIN.length() + expressionLength + INSERT_TAG_END.length(), afterLength);
					templateData = newTemplateData;
					i = begin + INSERT_TAG_BEGIN.length()
							+ expressionLength + INSERT_TAG_END.length();
					
//					char[] expression = Arrays.copyOfRange(templateData,
//							begin + 2, i);
//					char[] before = Arrays.copyOfRange(templateData, 0, begin);
//					char[] after = Arrays.copyOfRange(templateData, i + 1,
//							templateData.length);
//					char[] newTemplateData = new char[expression.length
//							+ before.length + after.length
//							+ INSERT_TAG_BEGIN.length()
//							+ INSERT_TAG_END.length()];
//					int index = 0;
//					for (int j = 0; j < before.length; j++) {
//						newTemplateData[index++] = before[j];
//					}
//					for (int j = 0; j < INSERT_TAG_BEGIN.length(); j++) {
//						newTemplateData[index++] = INSERT_TAG_BEGIN.charAt(j);
//					}
//					for (int j = 0; j < expression.length; j++) {
//						newTemplateData[index++] = expression[j];
//					}
//					for (int j = 0; j < INSERT_TAG_END.length(); j++) {
//						newTemplateData[index++] = INSERT_TAG_END.charAt(j);
//					}
//					for (int j = 0; j < after.length; j++) {
//						newTemplateData[index++] = after[j];
//					}
//
//					templateData = newTemplateData;
//					i = begin + INSERT_TAG_BEGIN.length() + expression.length
//							+ INSERT_TAG_END.length();
				}
			}

		}
		return templateData;
	}

	private static boolean encounterTag(char[] templateData, int cursor,
			char[] tag) {
		int tagIndex = 0;
		while (templateData[cursor] == tag[tagIndex]) {
			if (tagIndex == tag.length - 1) {
				return true;
			}
			tagIndex++;
			cursor++;
		}
		return false;
	}
}
