package entry;

import misc.L;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class CacheServer {
    public static final int FILE_MAGIC = 0x64766470;
    public static final int VERSION = 0x1;
    private final File file;

    private Map<String, BufferedImage> capsules;

    public CacheServer(String path){
        file = new File(path);
        capsules = new HashMap<>();
    }
    public static byte[] toByte(Object target){
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream out;
        final byte[] re;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(target);
            out.flush();
            re = bos.toByteArray();
        } catch (IOException | NullPointerException e){
            return new byte[0];
        } finally {
            try {
                bos.close();
            } catch (IOException ignored){}
        }
        return re;
    }
    public static Object toObject(byte[] bytes){
        final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in = null;
        final Object re;
        try {
            in = new ObjectInputStream(bis);
            re = in.readObject();
        } catch (IOException | NullPointerException | ClassNotFoundException e){
            return null;
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException ignored) {}
        }
        return re;
    }
    public void write(){
        synchronized (this){
            try {
                if (file.exists()) file.delete();
                file.createNewFile();
//                try (FileOutputStream fos = new FileOutputStream(file)){
//                    fos.write(FILE_MAGIC);
//                    fos.write(VERSION);
//                    byte[] serCapsules = toByte(capsules);
//                    fos.write(serCapsules.length);
//                    fos.write(serCapsules);
//                }
                try (RandomAccessFile raf = new RandomAccessFile(file, "rws")){
                    raf.writeInt(FILE_MAGIC);
                    raf.writeInt(VERSION);
                    byte[] serCapsules = toByte(capsules);
                    raf.writeInt(serCapsules.length);
                    raf.write(serCapsules);
                }
            } catch (IOException e) {
                L.log("CacheServer", "Can not create cache file");
            }
        }
    }
    public void flush() { write(); }

    public void restore(){
        try {
//            try  (FileInputStream fis = new FileInputStream(file)){
//
//            }
            if (!file.exists()) throw new RuntimeException("File not exists");
            RandomAccessFile raf = new RandomAccessFile(file, "r");
//            long offset = 0;
//            raf.seek(offset);
            int magic = raf.readInt();
            if (magic != FILE_MAGIC) throw new RuntimeException("FILE_MAGIC does not match");

//            offset += 4;
//            raf.seek(offset);
            int ver = raf.readInt();
            if (ver != VERSION) throw new RuntimeException("VERSION does not match");

//            offset += 4;
//            raf.seek(offset);
            int capsulesSize = raf.readInt();

//            offset += 4;
//            raf.seek(offset);
            final byte[] serializedCapsule = new byte[capsulesSize];
            raf.read(serializedCapsule, 0, capsulesSize);
            capsules = (Map<String, BufferedImage>) toObject(serializedCapsule);
            raf.close();
        } catch (IOException | RuntimeException e){
            L.log("CacheServer", "Can not restore cache file: %s".formatted(e.toString()));
        } finally {
            if (capsules == null) capsules = new HashMap<>();
        }
    }

    public Map<String, BufferedImage> getCapsules() {
        final Map<String, BufferedImage> re;
        synchronized (this) {
            re = new HashMap<>(capsules);
        }
        return re;
    }
    public Map<String, BufferedImage> getCapsulesReadOnly() {
        final Map<String, BufferedImage> re;
        synchronized (this) {
            re = capsules;
        }
        return re;
    }

    public void setCapsules(Map<String, BufferedImage> capsules) {
        synchronized (this){
            this.capsules = capsules;
        }
    }
}
