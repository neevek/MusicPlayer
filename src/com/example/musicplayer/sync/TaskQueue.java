package com.example.musicplayer.sync;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created with IntelliJ IDEA. 
 * User: neevek
 * Date: 7/20/13
 * Time: 18:58 PM
 */
public class TaskQueue extends Thread {
	private BlockingQueue<Object> mQueue;

	public TaskQueue(int initialCapacity) {
		mQueue = new LinkedBlockingQueue<Object>(initialCapacity);
	}

	@Override
	public synchronized void start() {
		super.start();
	}

	public synchronized void stopTaskQueue() {
		// use 'Poison Pill Shutdown' to stop the task queue
		// add a non-Runnable object, which will be recognized as the command
		// by the thread to break the infinite loop
		mQueue.add(new Object());
	}

	public synchronized void scheduleTask(Runnable task) {
		mQueue.add(task);
	}

	@Override
	public void run() {
		while (true) {
			try {
				Object obj = mQueue.take();
				if (obj instanceof Runnable)
					((Runnable) obj).run();
				else
					break;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
