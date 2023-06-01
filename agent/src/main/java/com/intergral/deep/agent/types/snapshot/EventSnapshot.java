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

package com.intergral.deep.agent.types.snapshot;

import com.intergral.deep.agent.IDUtils;
import com.intergral.deep.agent.Utils;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.types.TracePointConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EventSnapshot {

  private final String id;
  private final TracePointConfig tracepoint;
  private final Map<String, Variable> varLookup;
  private final long nanoTs;
  private final Collection<StackFrame> frames;
  private final ArrayList<WatchResult> watches;
  private long durationNanos;
  private final Resource resource;
  private Resource attributes;
  private boolean open;

  public EventSnapshot(final TracePointConfig tracepoint,
      final long nanoTs,
      final Resource resource,
      final Collection<StackFrame> frames,
      final Map<String, Variable> variables) {
    this.id = IDUtils.randomId();
    this.tracepoint = tracepoint;
    this.varLookup = new HashMap<>(variables);
    this.nanoTs = nanoTs;
    this.frames = frames;
    this.watches = new ArrayList<>();
    this.attributes = Resource.create(Collections.emptyMap());
    this.durationNanos = 0;
    this.resource = Resource.create(resource.getAttributes(), resource.getSchemaUrl());
    this.open = true;
  }

  public void addWatchResult(final WatchResult result, final Map<String, Variable> variables) {
    if (this.open) {
      this.watches.add(result);
      this.varLookup.putAll(variables);
    }
  }

  public void mergeAttributes(final Resource attributes) {
    if (this.open) {
      this.attributes = this.attributes.merge(attributes);
    }
  }

  public String getID() {
    return id;
  }

  public TracePointConfig getTracepoint() {
    return tracepoint;
  }

  public Map<String, Variable> getVarLookup() {
    return varLookup;
  }

  public long getNanoTs() {
    return nanoTs;
  }

  public Collection<StackFrame> getFrames() {
    return frames;
  }

  public ArrayList<WatchResult> getWatches() {
    return watches;
  }

  public long getDurationNanos() {
    return durationNanos;
  }

  public Resource getResource() {
    return resource;
  }

  public Resource getAttributes() {
    return attributes;
  }

  public void close() {
    this.open = false;
    final long currentTimeNano = Utils.currentTimeNanos()[1];
    this.durationNanos = currentTimeNano - this.nanoTs;
  }
}
