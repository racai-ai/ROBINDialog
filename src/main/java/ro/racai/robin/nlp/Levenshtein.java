/**
 * 
 */
package ro.racai.robin.nlp;

import java.util.HashMap;
import java.util.Map;

/**
 * Taken from:<br/>
 * <a href="https://rosettacode.org/wiki/Levenshtein_distance#Iterative_space_optimized_.28even_bounded.29">
 * https://rosettacode.org/wiki/Levenshtein_distance#Iterative_space_optimized_.28even_bounded.29</a>.</br>
 * Added cache for better performance.
 */
public class Levenshtein {
	private Map<String, Integer> levenshteinCache = new HashMap<String, Integer>();
	
	public int ld(String a, String b) {
		return distance(a, b, -1);
	}

	public boolean ld(String a, String b, int max) {
		return distance(a, b, max) <= max;
	}

	public int distance(String a, String b, int max) {
		String keyab = a + "#" + b;
		String keyba = b + "#" + a;
		
		if (levenshteinCache.containsKey(keyab)) {
			return levenshteinCache.get(keyab);
		}
		
		if (levenshteinCache.containsKey(keyba)) {
			return levenshteinCache.get(keyba);
		}
		
		if (a == b || a.equals(b)) {
			levenshteinCache.put(keyab, 0);
			return 0;
		}
		
		int la = a.length();
		int lb = b.length();
		
		if (max >= 0 && Math.abs(la - lb) > max) {
			levenshteinCache.put(keyab, max + 1);
			levenshteinCache.put(keyba, max + 1);
			
			return max + 1;
		}
		
		if (la == 0) {
			levenshteinCache.put(keyab, lb);
			levenshteinCache.put(keyba, lb);
			
			return lb;
		}
		
		if (lb == 0) {
			levenshteinCache.put(keyab, la);
			levenshteinCache.put(keyba, la);
			
			return la;
		}
		
		if (la < lb) {
			int tl = la;
			
			la = lb;
			lb = tl;
			
			String ts = a;
			
			a = b;
			b = ts;
		}

		int[] cost = new int[lb + 1];
		
		for (int i = 1; i <= la; i += 1) {
			cost[0] = i;
			
			int prv = i - 1;
			int min = prv;
			
			for (int j = 1; j <= lb; j += 1) {
				int act = prv + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
				
				cost[j] = min(1 + (prv = cost[j]), 1 + cost[j - 1], act);
				
				if (prv < min) {
					min = prv;
				}
			}
			
			if (max >= 0 && min > max) {
				levenshteinCache.put(keyab, max + 1);
				levenshteinCache.put(keyba, max + 1);
				
				return max + 1;
			}
		}
		
		if (max >= 0 && cost[lb] > max) {
			levenshteinCache.put(keyab, max + 1);
			levenshteinCache.put(keyba, max + 1);
			
			return max + 1;
		}
		
		levenshteinCache.put(keyab, cost[lb]);
		levenshteinCache.put(keyba, cost[lb]);
		
		return cost[lb];
	}

	private int min(int... a) {
		int min = Integer.MAX_VALUE;
		
		for (int i : a) {
			if (i < min) {
				min = i;
			}
		}
		
		return min;
	}
}
