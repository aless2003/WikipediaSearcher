package wiki;

public class WikipediaEntry {
  private final String url;
  private final String title;
  private final WikipediaEntry parent;
  private final int depth;

  public WikipediaEntry(String url, String title, WikipediaEntry parent) {
    this.url = url;
    this.title = title;
    this.parent = parent;

    if (parent == null) {
      this.depth = 0;
    } else {
      this.depth = parent.getDepth() + 1;
    }
  }

  public String getUrl() {
    return url;
  }

  @Override
  public String toString() {
    return "WikipediaEntry{" +
        "url='" + url + '\'' +
        ", title='" + title + '\'' +
        '}';
  }

  public String getPath() {
    if (parent == null) {
      return title;
    }

    return parent.getPath() + " -> " + title;
  }

  public int getDepth() {
    return depth;
  }
}
