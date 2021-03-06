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

package dev.gihwan.tollgate.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpRequest;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;

import dev.gihwan.tollgate.junit5.GatewayExtension;

class UpstreamTest {

    private static final AtomicReference<AggregatedHttpRequest> reqCapture = new AtomicReference<>();

    @RegisterExtension
    static final ServerExtension serviceServer = new ServerExtension() {
        @Override
        protected void configure(ServerBuilder sb) {
            sb.service("/foo",
                       (ctx, req) -> HttpResponse.from(req.aggregate().thenApply(aggregated -> {
                           reqCapture.set(aggregated);
                           return HttpResponse.of("bar");
                       })));
        }
    };

    @RegisterExtension
    static final GatewayExtension gateway = new GatewayExtension() {
        @Override
        protected void configure(GatewayBuilder builder) {
            builder.upstream("/foo", Upstream.of(serviceServer.httpUri()));
        }
    };

    @Test
    void proxy() {
        final WebClient webClient = WebClient.builder(gateway.httpUri()).build();

        final CompletableFuture<AggregatedHttpResponse> future = webClient.get("/foo").aggregate();
        await().atMost(Duration.ofSeconds(1)).until(future::isDone);
        final AggregatedHttpResponse res = future.join();
        assertThat(res.status()).isEqualTo(HttpStatus.OK);
        assertThat(res.contentUtf8()).isEqualTo("bar");

        final AggregatedHttpRequest req = reqCapture.get();
        assertThat(req.method()).isEqualTo(HttpMethod.GET);
        assertThat(req.path()).isEqualTo("/foo");
    }

    @Test
    void proxyWithBody() {
        final WebClient webClient = WebClient.builder(gateway.httpUri()).build();

        final CompletableFuture<AggregatedHttpResponse> future = webClient.post("/foo", "qux").aggregate();
        await().atMost(Duration.ofSeconds(1)).until(future::isDone);
        final AggregatedHttpResponse res = future.join();
        assertThat(res.status()).isEqualTo(HttpStatus.OK);
        assertThat(res.contentUtf8()).isEqualTo("bar");

        final AggregatedHttpRequest req = reqCapture.get();
        assertThat(req.method()).isEqualTo(HttpMethod.POST);
        assertThat(req.path()).isEqualTo("/foo");
        assertThat(req.contentUtf8()).isEqualTo("qux");
    }
}
