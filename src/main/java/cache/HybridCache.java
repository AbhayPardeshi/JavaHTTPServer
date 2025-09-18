package main.java.cache;


// what are we doing here
// 1. store many cacheEntry objects - 1 for each static file
// 2. decide if we can serve file from cache memory or from the disk - based on if we can find it in the memory
// 3. what file to evict/remove when our cache memory is full - use LRU and LCU
// 4. (optional for now) keep track of performance matrix(hit/miss/eviction)


import java.util.HashMap;
import java.util.Map;

public class HybridCache {
    private final int cacheLimit;
    private final Map<String,CacheEntry> cacheMemory;
    private final BloomFilter filter;

    private long hits = 0;
    private long misses = 0;
    private long evictions = 0;


    public HybridCache(int cacheLimit, BloomFilter filter){
        this.cacheLimit = cacheLimit;
        this.cacheMemory = new HashMap<>();
        this.filter = filter;
    }

    public synchronized byte[] getFile(String path){
        CacheEntry fileCache = cacheMemory.get(path);
        if(fileCache != null){
            hits++;
            fileCache.touch();
            return fileCache.getFileData();
        }

        misses++;
        return null;
    }

    public synchronized void putFile(String path, byte[] data){
        if(cacheMemory.size() >= cacheLimit){
            evict();
        }

        cacheMemory.put(path,new CacheEntry(data));
        filter.add(path);
    }


    // to learn various ways to implement evict function
    private void evict() {
        // eviction policy: blend LRU + LFU
        String victim = null;
        double victimScore = Double.MAX_VALUE;

        for (Map.Entry<String, CacheEntry> e : cacheMemory.entrySet()) {
            CacheEntry entry = e.getValue();
            long age = System.nanoTime() - entry.lastAccessTime;
            int freq = entry.frequency;

//          score = age / freq → lower = more valuable

//           Case 1: Age high, Freq high
//            Meaning: This file was used a lot in the past, but not recently.
//            Score: Depends on balance → age pushes it up, freq pulls it down.
//            Effect: Might still survive eviction if its frequency is huge.
//
//           Case 2: Age high, Freq low
//            Meaning: Old file that was rarely used.
//            Score: Very high (bad candidate).
//            Effect: Almost always evicted → "cold & useless".
//
//           Case 3: Age low, Freq high
//            Meaning: Recently accessed and also used often.
//            Score: Very low (good candidate).
//            Effect: Strongly protected from eviction → "hot & valuable".
//
//           Case 4: Age low, Freq low
//            Meaning: Recently accessed, but not many times overall.
//            Score: Moderate.
//            Effect: Stays for now, but if it doesn’t get reused soon, it’ll drift into Case 2.


            double score = (double) age / (freq + 1);

            if (score < victimScore) {
                victimScore = score;
                victim = e.getKey();
            }
        }
        if (victim != null) {
            cacheMemory.remove(victim);
            evictions++;
        }
    }

    // metrics
    public long getHits() { return hits; }
    public long getMisses() { return misses; }
    public long getEvictions() { return evictions; }
    public BloomFilter getFilter(){ return this.filter;}

}
