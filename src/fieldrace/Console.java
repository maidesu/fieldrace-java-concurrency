package fieldrace;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class Console {
	private Console() {}
	
	public static void printScoreboard() {
		try {
			while (FieldRace.isOn.get()) {
				System.out.println("Scores: " + sortedScores());
				Thread.sleep(1000);
			}
        } catch (InterruptedException ie) {
			System.err.println("Scoreboard got interrupted!");
			ie.printStackTrace();
			return;
		}
		System.out.println("Scoreboard has turned off.");
	}

	public static String sortedScores() {
		List<Map.Entry<Integer, Integer>> sortedList = new ArrayList<Map.Entry<Integer, Integer>>();

		// Copy to list
		for (int i = 0; i < FieldRace.PLAYER_COUNT; ++i) {
			sortedList.add(new AbstractMap.SimpleImmutableEntry<Integer, Integer>(i, FieldRace.scores.get(i)));
		}

		// Compare by value
		Collections.sort(sortedList, new Comparator<Map.Entry<Integer, Integer>>(){
			public int compare(Map.Entry<Integer, Integer> left, Map.Entry<Integer, Integer> right) {
                return -(left.getValue()).compareTo(right.getValue());
            }
		});

		return sortedList.toString();
	}
}
