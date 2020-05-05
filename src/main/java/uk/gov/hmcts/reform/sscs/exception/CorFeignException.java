package uk.gov.hmcts.reform.sscs.exception;

import static feign.Util.valuesOrEmpty;

import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.Util;
import java.io.IOException;

public class CorFeignException extends FeignException {
    public CorFeignException(String methodKey, Response response) {
        super(
                response.status(),
                methodKey + " caused an error\n" + responseToString(response),
                body(response)
        );
    }

    private static byte[] body(Response response) {
        byte[] body = {};
        try {
            if (response.body() != null) {
                body = Util.toByteArray(response.body().asInputStream());
            }
        } catch (IOException ignored) { // NOPMD
        }
        return body;
    }

    private static String responseToString(Response response) {
        StringBuilder builder = new StringBuilder("-----------------------------\n");
        Request request = response.request();
        if (request != null) {
            builder.append(request.httpMethod().name() + " " + request.url() + "\n");
        }
        builder.append("HTTP/1.1 ").append(response.status());
        if (response.reason() != null) {
            builder.append(' ').append(response.reason());
        }
        builder.append('\n');
        for (String field : response.headers().keySet()) {
            for (String value : valuesOrEmpty(response.headers(), field)) {
                builder.append(field).append(": ").append(value).append('\n');
            }
        }
        Response.Body body = response.body();
        if (body != null) {
            builder.append('\n');

            builder.append(new String(body(response)));
        }
        builder.append("\n-----------------------------");
        return builder.toString();
    }
}
