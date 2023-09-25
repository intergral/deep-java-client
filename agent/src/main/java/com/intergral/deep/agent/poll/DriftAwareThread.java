/*
 *     Copyright (C) 2023  Intergral GmbH
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.intergral.deep.agent.poll;

import com.intergral.deep.agent.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A thread that can run a {@link ITimerTask} accounting for drifting time.
 */
public class DriftAwareThread extends Thread {

  private static final Logger LOGGER = LoggerFactory.getLogger(DriftAwareThread.class);
  private final Object samplerLock = new Object();

  private final ITimerTask runnable;
  private final long interval;

  private volatile boolean active = true;
  private volatile long nextExecutionTime;


  /**
   * Create a new thread.
   *
   * @param name     the name for the thread
   * @param runnable the {@link ITimerTask} to execute
   * @param interval the interval in ms between each execution
   */
  public DriftAwareThread(final String name, final ITimerTask runnable, final long interval) {
    super(name);
    this.runnable = runnable;
    this.interval = interval;

    setDaemon(true);

    this.nextExecutionTime = System.currentTimeMillis() + interval;
  }


  private void stopSampler() {
    active = false;
  }


  /**
   * Start the thread using the provided delay.
   *
   * @param delay a delay in ms
   */
  public void start(final long delay) {
    final long now = System.currentTimeMillis();
    this.nextExecutionTime = now + delay;
    start();
  }


  @Override
  public void run() {
    while (active) {

      try {

        // calculate if we woke up early
        long[] nowTuple = Utils.currentTimeNanos();
        // the drift aware only calculates at ms accuracy - but we want to use ns for the time later
        long now = nowTuple[0];
        long startDelay = checkForEarlyWake(now, this.nextExecutionTime);

        // if we woke early
        // negative delay will cause exception
        // delay of zero will wait for ever
        while (startDelay > 0) {
          // and keep going until we are at the correct time
          trace("Next exe: " + this.nextExecutionTime);
          trace("Now: " + now);
          trace("Delaying for: " + startDelay);
          synchronized (samplerLock) {
            // sleep until it is time to run
            // wait can wake late so we wait for less time then the real delay
            final long timeout = startDelay / 4;
            if (timeout != 0) {
              trace("Wait for: " + timeout);
              samplerLock.wait(timeout);
            } else {
              trace("Wait for: " + startDelay);
              // the remaining delay is <= 4 milliseconds
              samplerLock.wait(startDelay);
            }
          }
          nowTuple = Utils.currentTimeNanos();
          now = nowTuple[0];
          startDelay = checkForEarlyWake(now, this.nextExecutionTime);

          // quick exit if we have been stopped
          if (!active) {
            return;
          }
        }

        final long startTime = System.currentTimeMillis();

        try {
          debug("Running task.");
          this.runnable.run(nowTuple[1]);
        } catch (final Exception e) {
          error("Exception during task execution: " + e.getMessage(), e);
        }

        final long after = System.currentTimeMillis();
        final long executionTime = after - startTime;
        this.nextExecutionTime = whatIsNextExecutionTime(this.nextExecutionTime, after);

        try {
          this.nextExecutionTime = this.runnable.callback(executionTime, this.nextExecutionTime);
        } catch (Exception e) {
          error("Exception during callback: " + e.getMessage(), e);
        }
        trace(String.format("Task complete; duration %dms next time %d",
            executionTime,
            this.nextExecutionTime));


      } catch (final Throwable t) {
        error("Exception during poll: " + t.getMessage(), t);
      }
    }
  }

  private void trace(final String msg) {
    LOGGER.trace(this.getName() + " - " + msg);
  }


  void error(final String msg, final Throwable throwable) {
    LOGGER.error(this.getName() + " - " + msg, throwable);
  }


  private void debug(final String msg) {
    LOGGER.debug(this.getName() + " - " + msg);
  }


  protected long whatIsNextExecutionTime(final long executionTime, final long now) {
    long nextExe = executionTime + this.interval;

    while (nextExe <= (now + 500)) {
      debug("The next execution time is in the past: " + nextExe);
      nextExe = nextExe + this.interval;
    }

    return nextExe;
  }


  protected long checkForEarlyWake(final long now, final long nextExecutionTime)
      throws InterruptedException {
    // correcting for early wake up
    if (nextExecutionTime != -1 && now < nextExecutionTime) {
      final long delay = nextExecutionTime - now;
      if (delay > 0) {
        return delay;
      }
    }
    return -1;
  }


  void stopTask() {
    synchronized (this.samplerLock) {
      this.stopSampler();
      this.samplerLock.notifyAll();
    }
  }
}