package fieldrace;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Checkpoint implements Runnable {
	final int id;
	
	Checkpoint(Integer id) {
		this.id = id;
	}

	@Override
	public void run() {
		while (FieldRace.isOn.get())
		{
			// Takes atomic integer from queue every 2 seconds
			AtomicInteger receivedAtomicInteger = null;
			do {
				if (FieldRace.isOn.get() == false)
				{
					System.out.println("Checkpoint " + this.id + " stopped polling, and turned off.");
					return;
				}

				try {
					receivedAtomicInteger = FieldRace.checkpointQueues.get(this.id).poll(2000, TimeUnit.MILLISECONDS);
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
