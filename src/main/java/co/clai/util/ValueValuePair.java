package co.clai.util;

import java.util.ArrayList;
import java.util.List;

public abstract class ValueValuePair {

	public abstract String getId();

	public abstract String getName();

	public static List<String> getIdsFromList(List<ValueValuePair> pairList) {
		List<String> retList = new ArrayList<>();

		for (ValueValuePair pair : pairList) {
			retList.add(pair.getId());
		}

		return retList;
	}
}
