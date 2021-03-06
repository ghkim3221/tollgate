/*
 * MIT License
 *
 * Copyright (c) 2020 - 2021 Gihwan Kim
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.gihwan.tollgate.remapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.linecorp.armeria.client.ClientRequestContext;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.util.SafeCloseable;
import com.linecorp.armeria.server.RoutingResult;
import com.linecorp.armeria.server.ServiceRequestContext;

class RemappingRequestPathStrategyTest {

    @Test
    void remapPath() {
        final RemappingRequestPathStrategy strategy = new RemappingRequestPathStrategy("/foo/bar");

        final HttpRequest req = HttpRequest.of(HttpMethod.GET, "/baz/qux/quux");
        final ClientRequestContext ctx = ClientRequestContext.of(req);

        final HttpRequest remappedReq = strategy.remap(ctx, req);
        assertThat(remappedReq.path()).isEqualTo("/foo/bar");
    }

    @Test
    void remapPathParam() {
        final RemappingRequestPathStrategy strategy = new RemappingRequestPathStrategy("/foo/{bar}");

        final HttpRequest req = HttpRequest.of(HttpMethod.GET, "/baz/qux/quux");
        final ServiceRequestContext serviceCtx =
                ServiceRequestContext.builder(req)
                                     .routingResult(RoutingResult.builder()
                                                                 .path(req.path())
                                                                 .decodedParam("bar", "quux")
                                                                 .build())
                                     .build();

        try (SafeCloseable ignored = serviceCtx.push()) {
            final ClientRequestContext ctx = ClientRequestContext.of(req);
            final HttpRequest remappedReq = strategy.remap(ctx, req);
            assertThat(remappedReq.path()).isEqualTo("/foo/quux");
        }
    }

    @Test
    void remapPathParamNotInServiceContext() {
        final RemappingRequestPathStrategy strategy = new RemappingRequestPathStrategy("/foo/{bar}");

        final HttpRequest req = HttpRequest.of(HttpMethod.GET, "/baz/qux/quux");
        final ClientRequestContext ctx = ClientRequestContext.of(req);
        assertThatThrownBy(() -> strategy.remap(ctx, req))
                .isInstanceOf(IllegalStateException.class);
    }
}
