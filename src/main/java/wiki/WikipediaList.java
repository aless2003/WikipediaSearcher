package wiki;

import java.util.HashSet;
import java.util.Set;

public class WikipediaList {
  Set<WikipediaEntry> entries;

  public WikipediaList() {
    this.entries = new HashSet<>();
  }

  public boolean containsLink(String link) {
    return entries.stream()
        .anyMatch(entry -> entry.getUrl().equals(link));
  }

  public void add(WikipediaEntry entry) {
    entries.add(entry);
  }

  @Override
  public String toString() {
    return "Entries: \n" + entries.toString();
  }
}
