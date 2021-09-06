package sbab.sl.api.aggregation;

import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class LinesFuture implements Future<Collection<Line>> {

	private final CountDownLatch latch;

	private final AtomicBoolean isDone;
	private final AtomicBoolean isCancelled;

	private Collection<Line> topList;

	LinesFuture() {
		this.isDone = new AtomicBoolean(false);
		this.isCancelled = new AtomicBoolean(false);
		this.latch = new CountDownLatch(1);
	}

	/*package*/ void complete(final Collection<Line> topList)
	{
		isDone.set(true);
		this.topList = topList;
		latch.countDown();
	}

	@Override
	public boolean cancel(final boolean mayInterruptIfRunning)
	{
		final boolean didCancel = !isDone.getAndSet(true);
		if (didCancel) {
			isCancelled.set(true);
		}
		latch.countDown();
		return didCancel;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled.get();
	}

	@Override
	public boolean isDone() {
		return isDone.get();
	}

	@Override
	public Collection<Line> get()
			throws InterruptedException, ExecutionException
	{
		latch.await();
		return topList;
	}

	@Override
	public Collection<Line> get(final long timeout, final TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException
	{
		latch.await(timeout, unit);
		return topList;
	}
}
