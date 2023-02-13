package fieldrace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FieldRace {
	// CONST
	public static final int PLAYER_COUNT = 10;
	public static final int CHECKPOINT_COUNT = 10;
	
	// VAR
	public static ExecutorService ex = Executors.newFixedThreadPool(PLAYER_COUNT + CHECKPOINT_COUNT + 1);
	public static AtomicBoolean isOn = new AtomicBoolean(true);
	public static ConcurrentHashMap<Integer, Integer> scores = new ConcurrentHashMap<>();
	public static AtomicInteger[] checkpointScores = new AtomicInteger[PLAYER_COUNT];
	public static List<BlockingQueue<AtomicInteger>> checkpointQueues = Collections.synchronizedList(new ArrayList<BlockingQueue<AtomicInteger>>(CHECKPOINT_COUNT));


	public static void main(String[] args) {
		for (int i = 0; i < PLAYER_COUNT; ++i) {
			// Set starting scores to 0
			scores.put(i, 0);
			// Initialize checkpoint scores to 0
			checkpointScores[i] = new AtomicInteger(0);
		}
		
		// Fill empty queues
		for (int i = 0; i < CHECKPOINT_COUNT; ++i)
			checkpointQueues.add(new ArrayBlockingQueue<AtomicInteger>(1));
		
		// Print scoreboard every second
		ex.submit(() -> Console.printScoreboard());
		
		// Checkpoints
		for (int i = 0; i < CHECKPOINT_COUNT; ++i)
			ex.submit(new Checkpoint(i));

		// Players
		for (int i = 0; i < PLAYER_COUNT; ++i)
			ex.submit(new Player(i));
		
		// After 10 seconds -> end
		try {
		Thread.sleep(10000);
		} catch(InterruptedException e) { e.printStackTrace(); }
		
		isOn.set(false);
		
		// Shutdown code from assignment
		ex.shutdown();
		try {
			ex.awaitTermination(3, TimeUnit.SECONDS);
		} catch (InterruptedException e) { e.printStackTrace(); }
		finally  {
			ex.shutdownNow();
			
			// Final scores
			System.out.println("Final Scores: " + Console.sortedScores());
		}
	}
}
