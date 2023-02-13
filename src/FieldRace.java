import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class FieldRace {
	
	// CONST
	static final int PLAYER_COUNT = 10;
	static final int CHECKPOINT_COUNT = 10;
	
	// VAR
	static ExecutorService ex = Executors.newFixedThreadPool(PLAYER_COUNT + CHECKPOINT_COUNT + 1);
	static AtomicBoolean isOn = new AtomicBoolean(true);
	static ConcurrentHashMap<Integer, Integer> scores = new ConcurrentHashMap<>();
	static AtomicInteger[] checkpointScores = new AtomicInteger[PLAYER_COUNT];
	static List<BlockingQueue<AtomicInteger>> checkpointQueues = Collections.synchronizedList(new ArrayList<BlockingQueue<AtomicInteger>>(CHECKPOINT_COUNT));

	// METHOD
	private static void printScoreboard() {
		try {
			while (isOn.get()) {
				System.out.println("Scores: " + sortedScores());
				Thread.sleep(1000);
			}
        } catch (InterruptedException ie) {
			System.err.println("Scoreboard got interrupted!");
			//ie.printStackTrace();
			return;
		}
		System.out.println("Scoreboard has turned off.");
	}

	private static String sortedScores() {
		List<Map.Entry<Integer, Integer>> sortedList = new ArrayList<Map.Entry<Integer, Integer>>();

		// Copy to list
		for (int i = 0; i < PLAYER_COUNT; ++i) {
			sortedList.add(new AbstractMap.SimpleImmutableEntry<Integer, Integer>(i, scores.get(i)));
		}

		// Compare by value
		Collections.sort(sortedList, new Comparator<Map.Entry<Integer, Integer>>(){
			public int compare(Map.Entry<Integer, Integer> left, Map.Entry<Integer, Integer> right) {
                return -(left.getValue()).compareTo(right.getValue());
            }
		});

		//Collections.reverse(sortedList);

		return sortedList.toString();
	}


	// NESTED CLASS
	private static class Player implements Runnable {
		final int id;
		
		Player(Integer id) {
			this.id = id;
		}

		@Override
   		public void run() {
			while (isOn.get())
			{
				// Selects checkpoint at random
				int nextCheckpoint = ThreadLocalRandom.current().nextInt(CHECKPOINT_COUNT);

				// Waits 0.5-2 seconds
				try {
					synchronized (this) {
						wait(ThreadLocalRandom.current().nextInt(1500+1) + 500);
					}
				}
				catch (InterruptedException ie) {
					System.err.println("Player " + this.id + " couldn't reach destination " + nextCheckpoint + "!");
					//ie.printStackTrace();
					return;
				}

				// Places checkpointScores[this.id] (of player) into checkpointQueues[nextCheckpoint] (of checkpoint)
				try {
					checkpointQueues.get(nextCheckpoint).put(checkpointScores[this.id]);
				}
				catch (InterruptedException ie)
				{
					// This may happen since every blockingqueue is currently set to 1 capacity on line 198
					System.err.println("Player " + this.id + " was interrupted while waiting for queue to empty at checkpoint " + nextCheckpoint + "!");
					//ie.printStackTrace();
					return;
				}
				
				final int score;
				
				// Waits for checkpoint notify
				do {
					if (isOn.get() == false)
					{
						System.out.println("Player " + this.id + " stopped waiting for checkpoint " + nextCheckpoint + ", and turned off.");
						return;
					}

					try {
						synchronized (checkpointScores[this.id])
						{
							checkpointScores[this.id].wait(3000);
						}
					}
					catch (InterruptedException ie)
					{
						System.err.println("Player " + this.id + " was interrupted while waiting for checkpoint " + nextCheckpoint + " notify!");
						//ie.printStackTrace();
						return;
					}
				} while (checkpointScores[this.id].get() == 0);

				score = checkpointScores[this.id].get();

				// Reset checkpointScore[this.id] to 0
				checkpointScores[this.id].set(0);

				System.out.println("Player " + this.id + " got " + score + " points at checkpoint " + nextCheckpoint);

				// Add score to scoreboard atomically
				scores.compute(this.id, (key, value) -> value + score);
			}
		}
	}

	private static class Checkpoint implements Runnable {
		final int id;
		
		Checkpoint(Integer id) {
			this.id = id;
		}

		@Override
   		public void run() {
			while (isOn.get())
			{
				// Takes atomic integer from queue every 2 seconds
				AtomicInteger receivedAtomicInteger = null;
				do {
					if (isOn.get() == false)
					{
						System.out.println("Checkpoint " + this.id + " stopped polling, and turned off.");
						return;
					}

					try {
						receivedAtomicInteger = checkpointQueues.get(this.id).poll(2000, TimeUnit.MILLISECONDS);
					}
					catch (InterruptedException ie)
					{
						System.err.println("Checkpoint " + this.id + " was interrupted while polling!");
						//ie.printStackTrace();
						return;
					}
				} while (receivedAtomicInteger == null);
				
				// Sets atomic integer to 10-100 value
				receivedAtomicInteger.set(ThreadLocalRandom.current().nextInt(90+1) + 10);

				// Notify player through atomic integer object
				synchronized (receivedAtomicInteger)
				{
					receivedAtomicInteger.notify();
				}
			}
		}
	}
	

	public static void main(String[] args) throws Exception {
		
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
		ex.submit(() -> printScoreboard());
		
		// Checkpoints
		for (int i = 0; i < CHECKPOINT_COUNT; ++i)
			ex.submit(new Checkpoint(i));

		// Players
		for (int i = 0; i < PLAYER_COUNT; ++i)
			ex.submit(new Player(i));
		
		// After 10 seconds -> end
		Thread.sleep(10000);
		isOn.set(false);
		
		// Shutdown code from assignment
		ex.shutdown();
		ex.awaitTermination(3, TimeUnit.SECONDS);
		ex.shutdownNow();
		
		// Final scores
		System.out.println("Final Scores: " + sortedScores());
	}
}
