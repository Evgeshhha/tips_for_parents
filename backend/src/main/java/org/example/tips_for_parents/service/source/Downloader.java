package org.example.tips_for_parents.service.source;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class Downloader {

    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public byte[] get(String url) throws IOException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "prikorm-bot/0.2 (+contact: you@example.com)")
                .timeout(Duration.ofMinutes(3))
                .GET().build();
        try {
            HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200) throw new IOException("HTTP " + resp.statusCode() + " для " + url);
            return resp.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Загрузка прервана: " + url, e);
        }
    }
}
