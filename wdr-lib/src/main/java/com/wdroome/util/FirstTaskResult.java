package com.wdroome.util;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.Random;

/**
 * Run a set of tasks in parallel, and get the result of the first one that succeeds.
 * Each task is defined by a Supplier that returns an object of type T.
 * Success is a non-null value.
 * @author wdr
 *
 * @param <T> The type resulted by the tasks.
 */
public class FirstTaskResult<T>
{
	/**
	 * Our thread. We create one for each task. The thread invokes the supplier,
	 * and returns the value to the controller via a blocking queue.
	 */
	private class Worker extends Thread
	{
		private final Supplier<T> m_task;
		private final ArrayBlockingQueue<ResultWrapper> m_queue;
		
		public Worker(Supplier<T> task, ArrayBlockingQueue<ResultWrapper> queue, String name)
		{
			m_task = task;
			m_queue = queue;
			if (name != null && !name.isBlank()) {
				setName(name);
			}
		}
		
		@Override
		public void run()
		{
			T result = null;
			try {
				result = m_task.get();
			} catch (Exception e) {
				result = null;
			} finally {
				try {
					// System.out.println("XXX: put " + result + " from " + getName());
					m_queue.add(new ResultWrapper(result));
				} catch (Exception e2) {
					// This shouldn't happen ... but ...
					if (!interrupted()) {
						e2.printStackTrace();
					}
				}
			}
		}
	}
	
	/**
	 * Wrap a possibly null result. We need this because Java's blocking queue
	 * does not accept null entries.
	 */
	private class ResultWrapper
	{
		private final T m_result;
		
		private ResultWrapper(T result)
		{
			m_result = result;
		}
	}
	
	/**
	 * Run a set of tasks in parallel, and return the first non-null value that any task returns.
	 * When a task returns a non-null value, the method will interrupt
	 * the remaining tasks. If interrupted, the tasks should die gracefully,
	 * and should clean up any side effects. 
	 * This does not tell the caller which task completed first;
	 * the caller must determine that by some other means.
	 * New threads are created for each invocation.
	 * If you need more control, use the thread pool tools in java.util.concurrent.
	 * @param tasks The tasks to perform in parallel.
	 * @param taskName An optional name for the threads created to run the tasks.
	 * @return The first non-null value that any task returns, or null if they all return null.
	 * @throws InterruptedException If the controlling thread is interrupted.
	 */
	public T get(List<Supplier<T>> tasks, String taskName) throws InterruptedException
	{
		if (tasks == null || tasks.isEmpty()) {
			return null;
		} else 	if (tasks.size() == 1) {
			// Optimize if there's only one task.
			return tasks.get(0).get();
		} else {
			final ArrayBlockingQueue<ResultWrapper> queue = new ArrayBlockingQueue<>(tasks.size());
			List<Worker> workers = new ArrayList<>(tasks.size());
			for (int iTask = 0; iTask < tasks.size(); iTask++) {
				Worker w = new Worker(tasks.get(iTask), queue, taskName);
				workers.add(w);
				w.start();
			}
			T result = null;
			for (int iResult = 0; iResult < tasks.size() && result == null; iResult++) {
				ResultWrapper r = queue.take();
				if (r != null) {
					result = r.m_result;
				}
			}
			if (result != null) {
				for (Worker worker: workers) {
					if (worker.isAlive()) {
						worker.interrupt();
					}
				}
			}
			return result;
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		test1();
		test2();
		test3();
		test4();
		test5();
	}
	
	public static void test1() throws Exception
	{
		System.out.println();
		System.out.println("Test 1:");
		FirstTaskResult<String> taskUtil = new FirstTaskResult<>();
		List<Supplier<String>> tasks = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			final int ix = i;
			tasks.add(() -> {
								try {
									Thread.sleep(ix*2000);
									return "Worker " + ix + " done.";
								} catch (Exception e) {
									System.out.println("Worker " + ix + " interrupted.");
									return null;
								}
							});
		}
		System.out.println("Result: " + taskUtil.get(tasks, "Test1"));
		Thread.sleep(1000);
	}
	
	public static void test2() throws Exception
	{
		System.out.println();
		System.out.println("Test 2:");
		FirstTaskResult<String> taskUtil = new FirstTaskResult<>();
		List<Supplier<String>> tasks = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			final int ix = i;
			tasks.add(() -> {
								try {
									Thread.sleep(ix*2000);
									return null;
								} catch (Exception e) {
									System.out.println("Worker " + ix + " interrupted.");
									return null;
								}
							});
		}
		System.out.println("Result: " + taskUtil.get(tasks, "Test2"));
		Thread.sleep(1000);
	}
	
	public static void test3() throws Exception
	{
		System.out.println();
		System.out.println("Test 3:");
		FirstTaskResult<String> taskUtil = new FirstTaskResult<>();
		List<Supplier<String>> tasks = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			final int ix = i;
			tasks.add(() -> {
								try {
									Thread.sleep(2000);
									return "Worker " + ix + " done.";
								} catch (Exception e) {
									System.out.println("Worker " + ix + " interrupted.");
									return null;
								}
							});
		}
		System.out.println("Result: " + taskUtil.get(tasks, "Test3"));
		Thread.sleep(1000);
	}
	
	public static void test4() throws Exception
	{
		System.out.println();
		System.out.println("Test 4:");
		FirstTaskResult<String> taskUtil = new FirstTaskResult<>();
		List<Supplier<String>> tasks = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			final int ix = i;
			tasks.add(() -> {
								try {
									Thread.sleep(ix*2000);
									return ix >= 3 ? "Worker " + ix + " done." : null;
								} catch (Exception e) {
									System.out.println("Worker " + ix + " interrupted.");
									return null;
								}
							});
		}
		System.out.println("Result: " + taskUtil.get(tasks, "Test4"));
		Thread.sleep(1000);
	}
	
	public static void test5() throws Exception
	{
		System.out.println();
		System.out.println("Test 5:");
		FirstTaskResult<String> taskUtil = new FirstTaskResult<>();
		List<Supplier<String>> tasks = new ArrayList<>();
		Random rand = new Random();
		for (int i = 1; i <= 5; i++) {
			final int ix = i;
			final boolean doit = rand.nextBoolean();
			System.out.println(" Worker " + ix + " " + doit);
			tasks.add(() -> {
								try {
									Thread.sleep(ix*2000);
									return doit ? "Worker " + ix + " done." : null;
								} catch (Exception e) {
									System.out.println("Worker " + ix + " interrupted.");
									return null;
								}
							});
		}
		System.out.println("Result: " + taskUtil.get(tasks, "Test5"));
		Thread.sleep(1000);
	}
}
