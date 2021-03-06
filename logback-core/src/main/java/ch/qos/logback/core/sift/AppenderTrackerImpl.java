/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2013, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.core.sift;

import java.util.*;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.CoreConstants;

/**
 * Track appenders by a key. When an appender is not used for
 * longer than THRESHOLD, stop it.
 *
 * @author Ceki Gulcu
 * @author Tommy Becker
 */
public class AppenderTrackerImpl<E> implements AppenderTracker<E> {

  static final boolean ACCESS_ORDERED = true;
  Map<String, Entry> map = new LinkedHashMap<String, Entry>(16, .75f, ACCESS_ORDERED);

  long lastCheck = 0;

  AppenderTrackerImpl() {
  }

  public synchronized void put(String key, Appender<E> value, long timestamp) {
    Entry entry = map.get(key);
    if (entry == null) {
      entry = new Entry(key, value, timestamp);
      map.put(key, entry);
    }
  }

  public synchronized Appender<E> get(String key, long timestamp) {
    Entry existing = map.get(key);
    if (existing == null) {
      return null;
    } else {
      existing.setTimestamp(timestamp);
      return existing.value;
    }
  }


  public synchronized void stopStaleAppenders(long now) {
    if (isTooSoon(now)) {
      return;
    }
    lastCheck = now;
    Iterator<Map.Entry<String, Entry>> iter = map.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<String, Entry> mapEntry = iter.next();
      Entry entry = mapEntry.getValue();
      if (isEntryStale(entry, now)) {
        iter.remove();
        Appender<E> appender = entry.value;
        appender.stop();
      } else {
        // if the first entry is not stale, then the following entries won't be stale either
        break;
      }
    }
  }

  private boolean isTooSoon(long now) {
    return (lastCheck + CoreConstants.MILLIS_IN_ONE_SECOND) > now;
  }

  /**
   * @param key
   * @since 0.9.19
   */
  public synchronized void endOfLife(String key) {
    Entry e = map.remove(key);
    if (e != null) {
      Appender<E> appender = e.value;
      appender.stop();
    }
  }

  public List<String> keyList() {
    return new ArrayList<String>(map.keySet());
  }


  private boolean isEntryStale(Entry entry, long now) {
    // stopped or improperly started appenders are considered stale
    // see also http://jira.qos.ch/browse/LBCLASSIC-316
    if (!entry.value.isStarted())
      return true;

    // unused appenders are also considered stale
    return ((entry.timestamp + THRESHOLD) < now);
  }


  public List<Appender<E>> valueList() {
    List<Appender<E>> result = new ArrayList<Appender<E>>();
    for (Entry entry : map.values()) {
      result.add(entry.value);
    }
    return result;
  }

  // ================================================================
  private class Entry {
    String key;
    Appender<E> value;
    long timestamp;

    Entry(String k, Appender<E> v, long timestamp) {
      this.key = k;
      this.value = v;
      this.timestamp = timestamp;
    }

    public void setTimestamp(long timestamp) {
      this.timestamp = timestamp;
    }

    @Override
    public int hashCode() {
      return key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      final Entry other = (Entry) obj;
      if (key == null) {
        if (other.key != null)
          return false;
      } else if (!key.equals(other.key))
        return false;
      if (value == null) {
        if (other.value != null)
          return false;
      } else if (!value.equals(other.value))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "(" + key + ", " + value + ")";
    }
  }
}
