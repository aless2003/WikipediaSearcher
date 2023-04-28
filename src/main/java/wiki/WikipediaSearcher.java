package wiki;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jsoup.Jsoup;

public class WikipediaSearcher {

  private final String startUrl;
  private final HttpClient client;

  private final WikipediaList visited = new WikipediaList();
  private final ExecutorService executor;
  private final String searchedTitle;
  private boolean isDone;
  private final String baseUrl;
  private final int maxDepth;

  public WikipediaSearcher(String startUrl, String searchedTitle, int maxDepth) {
    this.startUrl = startUrl;
    this.baseUrl = startUrl.substring(0, startUrl.indexOf("/wiki/"));
    this.maxDepth = maxDepth;

    this.searchedTitle = searchedTitle;
    this.client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build();
    this.executor = Executors.newFixedThreadPool(32);
    this.isDone = false;
  }

  public void search() {
    searchUrl(startUrl, null);
  }

  public void searchUrl(String url, WikipediaEntry parent) {
    if (isDone) {
      return;
    }

    if (visited.containsLink(url)) {
      return;
    }

    if (parent != null && parent.getDepth() >= maxDepth) {
      return;
    }


    System.out.println("Searching " + url + " at depth " + (parent == null ? 0 : parent.getDepth() + 1) + " ...");
    String pageSource = getPageSource(url);
    String title = parseTitle(pageSource);

    if (title.equals(searchedTitle)) {
      stop();
      System.out.println("Found " + searchedTitle + " at " + url);
      WikipediaEntry goal = new WikipediaEntry(url, title, parent);
      String path = goal.getPath();
      System.out.println("Path from start to goal:\n" + path);
      return;
    }

    List<String> links = parseLinks(pageSource);
    WikipediaEntry entry = new WikipediaEntry(url, title, parent);
    visited.add(entry);

    for (String link : links) {
      executor.submit(() -> searchUrl(baseUrl + link, entry));
    }
  }

  private void stop() {
    //write visited to file
    try {
      Files.write(Path.of("visited.txt"), visited.toString().getBytes());
    } catch (IOException e) {
      throw new RuntimeException("Error writing to file", e);
    }

    executor.shutdownNow();
    isDone = true;
  }

  private List<String> parseLinks(String pageSource) {
    var searchArea = Jsoup.parse(pageSource)
        .getElementsByClass("mw-parser-output")
        .get(0);

    return Jsoup.parse(searchArea.html())
        //#mw-content-text > div.mw-parser-output
        .select("a[href^=/wiki/]")
        .stream()
        .map(link -> link.attr("href"))
        .filter(link -> !link.contains("#"))
        .filter(link -> !link.contains(":"))
        .filter(link -> !visited.containsLink(link))
        .toList();
  }

  private String parseTitle(String pageSource) {
    int titleStart = pageSource.indexOf("<title>") + 7;
    int titleEnd = pageSource.indexOf("</title>");
    String title = pageSource.substring(titleStart, titleEnd);
    return title.replace(" - Wikipedia", "");
  }

  private String getPageSource(String url) {
    if (visited.containsLink(url)) {
      throw new IllegalArgumentException("Already visited " + url);
    }

    try {
      URI address = new URI(url);
      HttpRequest request = HttpRequest.newBuilder(address)
          .GET()
          .build();

      var response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new RuntimeException("Error connecting to " + url + ": " + response.statusCode());
      }

      return response.body();

    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid URL: " + url);
    } catch (IOException e) {
      throw new RuntimeException("Error connecting to " + url, e);
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while connecting to " + url, e);
    }
  }

  public void waitUntilDone() {
    while (!isDone) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new RuntimeException("Interrupted while waiting for searcher to finish", e);
      }
    }
  }
}
