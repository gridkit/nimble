package org.gridkit.nimble.zootest;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PropHelper {

	public static Properties filterByPrefix(String prefix, Properties props) {
		Pattern p = GlobHelper.translate(prefix, ".");
		Properties nprops = new Properties();
		for(Object o: props.keySet()) {
			String key = (String)o;
			Matcher matcher = p.matcher(key);
			if (matcher.lookingAt()) {
				String match = matcher.group();
				String nkey = key.substring(match.length());
				if (nkey.startsWith(".")) {
					nkey = nkey.substring(1);
				}
				nprops.put(nkey, props.get(key));
			}
		}
		return nprops;				
	}
}
