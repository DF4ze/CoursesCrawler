package fr.ses10doigts.coursesCrawler.service.scrap.tool;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FieldTool
{
	public static boolean isInteger(String value)
	{
		boolean isInteger = true;
		try
		{
			Integer.parseInt(value);
		}
		catch(Exception e)
		{
			isInteger = false;
		}
		
		return isInteger;
	}
	
	public static boolean containsLetterOrDigit(String value)
	{
		for (int index = 0; index < value.length(); index++)
		{
			char chr = value.charAt(index);
			
			if ((chr >= '0' && chr <= '9') || (chr >= 'a' && chr <= 'z') || (chr >= 'A' && chr <= 'Z'))
			{
				return true;
			}
		}
		
		return false;
	}

	public static Long extractCode(String url) {
		Pattern pattern = Pattern.compile("_c(\\d+)$");
		Matcher matcher = pattern.matcher(url);

		Long found = null;
		if (matcher.find()) {
			String code = matcher.group(1);
			found = Long.parseLong(code);

		}else{
			pattern = Pattern.compile("id_course=(\\d+)$");
			matcher = pattern.matcher(url);
			if (matcher.find()) {
				String code = matcher.group(1);
				found = Long.parseLong(code);

			}
		}

		return found;
	}
}
