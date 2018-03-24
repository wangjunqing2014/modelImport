package com.wjq;

import com.wjq.entity.PprtDgMetaCore;
import com.wjq.entity.TableEntity;
import com.xiaoleilu.hutool.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ExecutorServiceUtil {

	public static ExecutorService executorService;
	public static CompletionService<HashMap<String, String>> completionService;
	public static int QUEUE_MAX_SIZE = 500;
	public static double BLOCKING_COEFFICIENT = 0.7;

	static{
		// 最佳的线程数 = CPU可用核心数 / (1 - 阻塞系数)
		int poolSize = (int) (Runtime.getRuntime().availableProcessors() / (1 - BLOCKING_COEFFICIENT));
		executorService = new ThreadPoolExecutor(poolSize, poolSize,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(QUEUE_MAX_SIZE),
				new SelfDefinedThreadFactory("wjqThread"), new ThreadPoolExecutor.AbortPolicy());
		completionService = ThreadUtil.newCompletionService(executorService);
	}

	static class SelfDefinedThreadFactory implements ThreadFactory {
		final AtomicInteger threadNumber = new AtomicInteger(1);
		final String namePrefix;

		SelfDefinedThreadFactory(String namePrefix) {
			this.namePrefix = namePrefix+"-";
		}

		public Thread newThread(Runnable r) {
			Thread t = new Thread( r,namePrefix + threadNumber.getAndIncrement());
			if (t.isDaemon())
				t.setDaemon(true);
			if (t.getPriority() != Thread.NORM_PRIORITY)
				t.setPriority(Thread.NORM_PRIORITY);
			return t;
		}
	}
}
