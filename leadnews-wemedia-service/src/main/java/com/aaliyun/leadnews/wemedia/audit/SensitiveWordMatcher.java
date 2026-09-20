package com.aaliyun.leadnews.wemedia.audit;

import java.util.*;

public final class SensitiveWordMatcher {

 private static final int MAX_MATCHES = 20;

 private final Node root;
 public SensitiveWordMatcher(Collection<String> words) {
  Node mutable = new Node();
  root = mutable;
  words.forEach(this::add);
 }

 public SensitiveWordMatchResult match(String text) {
  if (text == null || text.isEmpty()) {
   return SensitiveWordMatchResult.none();
  }

  LinkedHashSet<String> found = new LinkedHashSet<>();
  for (int i = 0; i < text.length() && found.size() < MAX_MATCHES; i++) {
   Node n = root;
   for (int j = i; j < text.length(); j++) {
    n = n.next.get(text.charAt(j));
    if (n == null) {
     break;
    }
    if (n.word != null) {
     found.add(n.word);
     break;
    }
   }
  }
  return new SensitiveWordMatchResult(!found.isEmpty(), List.copyOf(found));
 }

 private void add(String word) {
  if (word == null || word.isBlank()) {
   return;
  }
  Node n = root;
  for (char c : word.toCharArray()) {
   n = n.next.computeIfAbsent(c, k -> new Node());
  }
  n.word = word;
 }

 private static final class Node {
  final Map<Character, Node> next = new HashMap<>();
  String word;
 }
}
