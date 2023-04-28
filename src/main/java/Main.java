import wiki.WikipediaSearcher;

public class Main {

  public static void main(String[] args) {
    String sourceLink = "https://en.wikipedia.org/wiki/Hatsune_Miku";
    String searchedTitle = "Online shopping";
    WikipediaSearcher searcher = new WikipediaSearcher(sourceLink, searchedTitle, 3);
    searcher.search();
    searcher.waitUntilDone();
  }
}
