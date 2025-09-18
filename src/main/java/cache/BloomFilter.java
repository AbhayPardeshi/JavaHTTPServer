package main.java.cache;

import java.util.BitSet;

public class BloomFilter {
    private final BitSet bitset;
    private final int size;
    private final int hashCount;    // number of times we want the hash function to run

    public BloomFilter(int size, int hashCount) {
        this.size = size;
        this.hashCount = hashCount;
        this.bitset = new BitSet(size);
    }

    public void add(String key) {
        for (int i = 0; i < hashCount; i++) {
            int hash = hash(key, i);   //will generate a large number
            bitset.set(Math.abs(hash % size), true);   // convert that number between (0 to size-1)
        }
    }

    public boolean mightContain(String key) {
        for (int i = 0; i < hashCount; i++) {
            int hash = hash(key, i);
            if (!bitset.get(Math.abs(hash % size))) {  // check if we have true for that bit in our bitset
                return false;
            }
        }
        return true; // if we get true for all the time hash function runs - it tells that this route might be in cache memory
    }

    private int hash(String key, int seed) {
        return key.hashCode() ^ (seed * 0x9e3779b9); // generates a unique hash code - sometimes may be few collision but acceptable in bloom filter
    }
}
