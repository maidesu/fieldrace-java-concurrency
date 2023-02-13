package fieldrace;

import java.util.concurrent.ThreadLocalRandom;

public class Player implements Runnable {
	final int id;
	
	Player(Integer id) {
		this.id = id;
	}

	@Override
	public void run() {
		while (FieldRace.isOn.get())
		{
			// Selects checkpoint at random
			int nextCheckpoint = ThreadLocalRandom.current().nextInt(FieldRace.CHECKPOINT_COUNT);
	
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
				FieldRace.checkpointQueues.get(nextCheckpoint).put(FieldRace.checkpointScores[this.id]);
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
				if (FieldRace.isOn.get() == false)
				{
					System.out.println("Player " + this.id + " stopped waiting for checkpoint " + nextCheckpoint + ", and turned off.");
					return;
				}
	
				try {
					synchronized (FieldRace.checkpointScores[this.id])
					{
						FieldRace.checkpointScores[this.id].wait(3000);
					}
				}
				catch (InterruptedException ie)
				{
					System.err.println("Player " + this.id + " was interrupted while waiting for checkpoint " + nextCheckpoint + " notify!");
					//ie.printStackTrace();
					return;
				}
			} while (FieldRace.checkpointScores[this.id].get() == 0);
	
			score = FieldRace.checkpointScores[this.id].get();
	
			// Reset checkpointScore[this.id] to 0
			FieldRace.checkpointScores[this.id].set(0);
	
			System.out.println("Player " + this.id + " got " + score + " points at checkpoint " + nextCheckpoint);
	
			// Add score to scoreboard atomically
			FieldRace.scores.compute(this.id, (key, value) -> value + score);
		}
	}
}
