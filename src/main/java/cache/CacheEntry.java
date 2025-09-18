package main.java.cache;

public class CacheEntry {
    byte[] data;                     // used to store the file in bytes
    long lastAccessTime;            // when was the last time this file served - mainly for LRU
    int frequency;                  // how many times has this file been served - for LCU

    CacheEntry(byte[] data){
        this.data = data;
        this.lastAccessTime = System.nanoTime();
        this.frequency = 1;
    }


    // whenever a file is served from cache update lastAccess and frequency
    public void touch(){
        this.lastAccessTime = System.nanoTime();
        this.frequency++;
    }

    public byte[] getFileData(){
        return this.data;
    }
}
