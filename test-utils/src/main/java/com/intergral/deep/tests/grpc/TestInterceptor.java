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

package com.intergral.deep.tests.grpc;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

public class TestInterceptor implements ServerInterceptor {

  private final String key;
  private Context.Key<String> contextKey;

  public TestInterceptor(final String key) {
    this.key = key;
  }

  public Context.Key<String> contextKey() {
    if (contextKey == null) {
      contextKey = Context.key(key);
    }
    return contextKey;
  }

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(final ServerCall<ReqT, RespT> call, final Metadata headers,
      final ServerCallHandler<ReqT, RespT> next) {
    final Key<String> grpcKey = Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
    final String val = headers.get(grpcKey);
    final Context context = Context.current().withValue(contextKey(), val);
    return Contexts.interceptCall(context, call, headers, next);
  }
}
