package entry;

import com.cycastic.javabase.firestore.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class CapsulesJob {
    public final int CACHE_SIZE = 30;
    private final CacheServer cacheServer;
    private final Map<String, BufferedImage> cachedCapsules;
    public void batchFetch(String[] urls){
        new Thread(() -> {
            for (String url : urls) fetch(url);
        }).start();
    }
    public BufferedImage fetch(String url){
        BufferedImage re;
        synchronized (this){
            if (cachedCapsules.containsKey(url)) {
                re = cachedCapsules.get(url);
            } else {
                try {
                    URL capsule_url = new URL(url);
                    re = ImageIO.read(capsule_url);
                    cachedCapsules.put(url, re);
                    if (cachedCapsules.size() > CACHE_SIZE){
                        cachedCapsules.entrySet().iterator().remove();
                    }
                } catch (IOException e){
                    re = null;
                }
            }
        }
        return re;
    }
    public void flush(){
        cacheServer.setCapsules(cachedCapsules);
    }

    public CapsulesJob(CacheServer cache){
        cacheServer = cache;
        cachedCapsules = cacheServer.getCapsules();
    }
}
