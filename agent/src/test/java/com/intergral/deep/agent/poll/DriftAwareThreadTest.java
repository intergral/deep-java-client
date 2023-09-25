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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DriftAwareThreadTest {

  private DriftAwareThread task;


  @BeforeEach
  public void setUp() {
    this.task = new DriftAwareThread("Test", null, 1000);
  }


  @Test
  public void delay2() throws Exception {
    class LWrap {

      long now;
    }

    final LWrap lwrap = new LWrap();
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    final long currentTimeMillis = System.currentTimeMillis();
    final DriftAwareThread driftAwareThread = new DriftAwareThread("Test", new ITimerTask() {
      @Override
      public void run(final long now) {
        assertTrue((now + 10000) >= currentTimeMillis);
        lwrap.now = now;
        countDownLatch.countDown();
      }


      @Override
      public long callback(final long duration, final long next) {
        return next;
      }
    }, 5000);
    driftAwareThread.start(10000);
    countDownLatch.await();
    driftAwareThread.stopTask();
    assertTrue((lwrap.now + 10000) >= currentTimeMillis);
  }

  @Test
  void errorLogs() {
    final DriftAwareThread spy = Mockito.spy(task);
    spy.error("test error", new RuntimeException("test exception"));
    Mockito.verify(spy, Mockito.times(1)).getName();
  }

  @Test
  public void testCheckForEarlyWakeUp() throws Exception {
    assertEquals(1000, task.checkForEarlyWake(0, 1000));
    assertEquals(999, task.checkForEarlyWake(1, 1000));
    assertEquals(100, task.checkForEarlyWake(900, 1000));
    assertEquals(1, task.checkForEarlyWake(999, 1000));
    assertEquals(-1, task.checkForEarlyWake(1000, 1000));
    assertEquals(-1, task.checkForEarlyWake(1001, 1000));
    assertEquals(-1, task.checkForEarlyWake(1100, 1000));
    assertEquals(-1, task.checkForEarlyWake(2000, 1000));

    final long now = System.currentTimeMillis();

    assertEquals(1000, task.checkForEarlyWake(now, now + 1000));

  }


  @Test
  public void testWhatIsNextExecutionTime() {
    assertEquals(2000, task.whatIsNextExecutionTime(1000, 1001));
    assertEquals(2000, task.whatIsNextExecutionTime(1000, 1499));
    assertEquals(3000, task.whatIsNextExecutionTime(1000, 1500));
    assertEquals(3000, task.whatIsNextExecutionTime(1000, 1501));

    assertEquals(1000, task.whatIsNextExecutionTime(0, 0));
    assertEquals(1000, task.whatIsNextExecutionTime(0, 100));
    assertEquals(2000, task.whatIsNextExecutionTime(0, 1000));

    assertEquals(2000, task.whatIsNextExecutionTime(1000, 1010));
    assertEquals(2100, task.whatIsNextExecutionTime(1100, 1));
    assertEquals(2110, task.whatIsNextExecutionTime(1110, 1));
    assertEquals(2111, task.whatIsNextExecutionTime(1111, 1));

    assertEquals(4000, task.whatIsNextExecutionTime(1000, 3000));
    assertEquals(4000, task.whatIsNextExecutionTime(1000, 2999));

    final long now = System.currentTimeMillis();

    assertEquals(now + 1000, task.whatIsNextExecutionTime(now, now));
    assertEquals(now + 1000, task.whatIsNextExecutionTime(now, now - 1000));
    assertEquals(now, task.whatIsNextExecutionTime(now - 1000, now - 1000));
    assertEquals(now + 1000, task.whatIsNextExecutionTime(now - 1000, now));

    assertEquals(now + 2000, task.whatIsNextExecutionTime(now, now + 1000));
    assertEquals(now + 2000, task.whatIsNextExecutionTime(now + 1000, now));
    assertEquals(now + 2000, task.whatIsNextExecutionTime(now + 1000, now + 1000));
  }
}