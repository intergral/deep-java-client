/*
 *    Copyright 2023 Intergral GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.intergral.deep.examples;


public class Main {

  public static void main(String[] args) throws Throwable {
    final SimpleTest ts = new SimpleTest("This is a test", 2);
    for (; ; ) {
      try {
        ts.message(ts.newId());
      } catch (Exception e) {
        e.printStackTrace();
      }

      Thread.sleep(1000);
    }
  }
}
