import ru.netology.server.Server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Main {
    private static final int PORT = 8080;
    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String ENDPOINT_CLASSIC_HTML = "/classic.html";


    public static void main(String[] args) {
        var server = new Server(64);

        server.addHandler(METHOD_GET, ENDPOINT_CLASSIC_HTML, (request, out) -> {
            try {
                final var filePath = Path.of(".", "/src/main/resources", request.getPath());
                final var mimeType = Files.probeContentType(filePath);

                // special case for classic
                final var template = Files.readString(filePath);
                final var content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ).getBytes();
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.write(content);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        server.addHandler(METHOD_POST, ENDPOINT_CLASSIC_HTML, (request, out) -> {
            try {
                final var filePath = Path.of(".", "/src/main/resources", request.getPath());
                final var mimeType = Files.probeContentType(filePath);

                // special case for classic
                final var template = Files.readString(filePath);

                // show actual time
                // show querry params in body
                final var content = template
                        .replace("{time}", LocalDateTime.now().toString())
                        .replace("{params}", request.getQuerryStringBodyParams().toString())
                        .getBytes();

                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.write(content);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        server.listen(PORT);
    }
}