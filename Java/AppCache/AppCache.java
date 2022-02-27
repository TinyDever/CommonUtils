import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.bubiu.counter.entity.StartThread;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Michael Yang（www.yangfuhai.com） update at 2013.08.07
 */
public final class  AppCache {
   private static final int TIME_HOUR = 60 * 60;
   public static final int TIME_MIN = 60;
   public static final int TIME_DAY = TIME_HOUR * 24;

    private static final int MAX_SIZE = 1000 * 1000 * 50;
    private static final int MAX_COUNT = Integer.MAX_VALUE;
    private static Map<String, AppCache> mInstanceMap = new HashMap<>();
    private Manager mCache;

    public static AppCache get(Context ctx) {
        return get(ctx, "AppCache");
    }

    public static AppCache get(Context ctx, String cacheName) {
        File f = new File(ctx.getCacheDir(), cacheName);
        return get(f, MAX_SIZE, MAX_COUNT);
    }

    public static AppCache get(File cacheDir) {
        return get(cacheDir, MAX_SIZE, MAX_COUNT);
    }

    public static AppCache get(Context ctx, long maxSize, int maxCount) {
        File f = new File(ctx.getCacheDir(), "AppCache");
        return get(f, maxSize, maxCount);
    }

    public static AppCache get(File cacheDir, long maxSize, int maxCount) {
        AppCache manager = mInstanceMap.get(cacheDir.getAbsoluteFile() + myPid());
        if (manager == null) {
            manager = new AppCache(cacheDir, maxSize, maxCount);
            mInstanceMap.put(cacheDir.getAbsolutePath() + myPid(), manager);
        }
        return manager;
    }

    private static String myPid() {
        return "_" + android.os.Process.myPid();
    }

    private AppCache(File cacheDir, long maxSize, int maxCount) {
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            throw new RuntimeException("can't make dirs in "
                    + cacheDir.getAbsolutePath());
        }
        mCache = new Manager(cacheDir, maxSize, maxCount);
    }

    private static class Utils {
        private static final int STR_LEN = 2;
        private static final String STR_START_WITH = "0";
        private static final int THOUSAND = 1000;
        private static final int STAND_LEN = 13;

       /**
        * 判断缓存的String数据是否到期
        * @return true：到期了 false：还没有到期
        */
       private static boolean isDue(String str) {
           return isDue(str.getBytes());
       }

        /**
         * 判断缓存的byte数据是否到期
         * @return true：到期了 false：还没有到期
         */
        private static boolean isDue(byte[] data) {
            String[] str = getDateInfoFromDate(data);
            if (str != null && str.length == STR_LEN) {
                String saveTimeStr = str[0];
                while (saveTimeStr.startsWith(STR_START_WITH)) {
                    saveTimeStr = saveTimeStr
                            .substring(1, saveTimeStr.length());
                }
                long saveTime = Long.valueOf(saveTimeStr);
                long deleteAfter = Long.valueOf(str[1]);
                return System.currentTimeMillis() > saveTime + deleteAfter * THOUSAND;
            }
            return false;
        }

       private static String newStringWithDateInfo(int second, String strInfo) {
           return createDateInfo(second) + strInfo;
       }

        private static byte[] newByteArrayWithDateInfo(int second, byte[] data2) {
            byte[] data1 = createDateInfo(second).getBytes();
            byte[] retData = new byte[data1.length + data2.length];
            System.arraycopy(data1, 0, retData, 0, data1.length);
            System.arraycopy(data2, 0, retData, data1.length, data2.length);
            return retData;
        }

       private static String clearDateInfo(String strInfo) {
           if (strInfo != null && hasDateInfo(strInfo.getBytes())) {
               strInfo = strInfo.substring(strInfo.indexOf(M_SEPARATOR) + 1,
                       strInfo.length());
           }
           return strInfo;
       }

        private static byte[] clearDateInfo(byte[] data) {
            if (hasDateInfo(data)) {
                return copyOfRange(data, indexOf(data, M_SEPARATOR) + 1,
                        data.length);
            }
            return data;
        }

        private static boolean hasDateInfo(byte[] data) {
            return data != null && data.length > 15 && data[13] == '-'
                    && indexOf(data, M_SEPARATOR) > 14;
        }

        private static String[] getDateInfoFromDate(byte[] data) {
            if (hasDateInfo(data)) {
                String saveDate = new String(copyOfRange(data, 0, 13));
                String deleteAfter = new String(copyOfRange(data, 14,
                        indexOf(data, M_SEPARATOR)));
                return new String[]{saveDate, deleteAfter};
            }
            return null;
        }

        private static int indexOf(byte[] data, char c) {
            for (int i = 0; i < data.length; i++) {
                if (data[i] == c) {
                    return i;
                }
            }
            return -1;
        }

        private static byte[] copyOfRange(byte[] original, int from, int to) {
            int newLength = to - from;
            if (newLength < 0) {
                throw new IllegalArgumentException(from + " > " + to);
            }
            byte[] copy = new byte[newLength];
            System.arraycopy(original, from, copy, 0,
                    Math.min(original.length - from, newLength));
            return copy;
        }

        private static final char M_SEPARATOR = ' ';

        private static String createDateInfo(int second) {
            StringBuilder currentTime = new StringBuilder(System.currentTimeMillis() + "");
            while (currentTime.length() < STAND_LEN) {
                currentTime.insert(0, STR_START_WITH);
            }
            return currentTime + "-" + second + M_SEPARATOR;
        }

        private static byte[] bitmap2Bytes(Bitmap bm) {
            if (bm == null) {
                return null;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
            return baos.toByteArray();
        }

       private static Bitmap bytes2Bitmap(byte[] b) {
           if (b.length == 0) {
               return null;
           }
           return BitmapFactory.decodeByteArray(b, 0, b.length);
       }

       private static Bitmap drawable2Bitmap(Drawable drawable) {
           if (drawable == null) {
               return null;
           }
           //取 drawable 的长宽
           int w = drawable.getIntrinsicWidth();
           int h = drawable.getIntrinsicHeight();
           //取 drawable 的颜色格式
           Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                   : Bitmap.Config.RGB_565;
           //建立对应 bitmap
           Bitmap bitmap = Bitmap.createBitmap(w, h, config);
           //建立对应 bitmap 的画布
           Canvas canvas = new Canvas(bitmap);
           drawable.setBounds(0, 0, w, h);
           //把 drawable 内容画到画布中
           drawable.draw(canvas);
           return bitmap;
       }

       private static Drawable bitmap2Drawable(Bitmap bm) {
           if (bm == null) {
               return null;
           }
           return new BitmapDrawable(Resources.getSystem(), bm);
       }
    }

    /**
     * put byte array
     *
     * @param key   key value
     * @param value data value
     */
    private void putByteArray(String key, byte[] value) {
        File file = mCache.newFile(key);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out.write(value);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mCache.put(file);
        }
    }

    private void putByteArray(String key, byte[] value, int saveTime) {
        putByteArray(key, Utils.newByteArrayWithDateInfo(saveTime, value));
    }

    private byte[] getByteArray(String key) {
        RandomAccessFile raFile = null;
        boolean removeFile = false;
        try {
            File file = mCache.get(key);
            if (!file.exists()) {
                return null;
            }
            raFile = new RandomAccessFile(file, "r");
            byte[] byteArray = new byte[(int) raFile.length()];
            raFile.read(byteArray);
            if (!Utils.isDue(byteArray)) {
                return Utils.clearDateInfo(byteArray);
            } else {
                removeFile = true;
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (raFile != null) {
                try {
                    raFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (removeFile) {
                remove(key);
            }
        }
    }

    /**
     * 保存 Serializable数据 到 缓存中
     *
     * @param key   保存的key
     * @param value 保存的value
     */
    public void putSerializable(String key, Serializable value) {
        putSerializable(key, value, -1);
    }

    /**
     * 保存 Serializable数据到 缓存中
     *
     * @param key      保存的key
     * @param value    保存的value
     * @param saveTime 保存的时间，单位：秒
     */
    private void putSerializable(String key, Serializable value, int saveTime) {
        ByteArrayOutputStream bs;
        ObjectOutputStream oos = null;
        try {
            bs = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bs);
            oos.writeObject(value);
            byte[] data = bs.toByteArray();
            if (saveTime != -1) {
                putByteArray(key, data, saveTime);
            } else {
                putByteArray(key, data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public <T extends Serializable> T getSerializable(String key) {
        byte[] data = getByteArray(key);
        if (data != null) {
            ByteArrayInputStream bs = null;
            ObjectInputStream ois = null;
            try {
                bs = new ByteArrayInputStream(data);
                ois = new ObjectInputStream(bs);
                return (T) ois.readObject();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                try {
                    if (bs != null) {
                        bs.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    if (ois != null) {
                        ois.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;

    }


    void putBitmap(String key, Bitmap value) {
        putByteArray(key, Utils.bitmap2Bytes(value));
    }
    /**
     * 获取缓存文件
     *
     * @return value 缓存的文件
     */
    public File file(String key) {
        File f = mCache.newFile(key);
        if (f.exists()) {
            return f;
        }
        return null;
    }

    /**
     * 移除某个key
     *
     * @return 是否移除成功
     */
    public boolean remove(String key) {
        return mCache.remove(key);
    }


    /**
     * 缓存管理器
     *
     * @author 杨福海（michael） www.yangfuhai.com
     * @version 1.0
     */
    private class Manager {
        private final AtomicLong cacheSize;
        private final AtomicInteger cacheCount;
        private final long sizeLimit;
        private final int countLimit;
        private final Map<File, Long> lastUsageDates = new HashMap<>();
        private File cacheDir;

        private Manager(File cacheDir, long sizeLimit, int countLimit) {
            this.cacheDir = cacheDir;
            this.sizeLimit = sizeLimit;
            this.countLimit = countLimit;
            cacheSize = new AtomicLong();
            cacheCount = new AtomicInteger();
            calculateCacheSizeAndCacheCount();
        }

        /**
         * 计算 cacheSize和cacheCount
         */
        private void calculateCacheSizeAndCacheCount() {
            StartThread.start(() -> {
                int size = 0;
                int count = 0;
                File[] cachedFiles = cacheDir.listFiles();
                if (cachedFiles != null) {
                    for (File cachedFile : cachedFiles) {
                        size += Manager.this.calculateSize(cachedFile);
                        count += 1;
                        lastUsageDates.put(cachedFile,
                                cachedFile.lastModified());
                    }
                    cacheSize.set(size);
                    cacheCount.set(count);
                }
            });
        }

        private void put(File file) {
            int curCacheCount = cacheCount.get();
            while (curCacheCount + 1 > countLimit) {
                long freedSize = removeNext();
                cacheSize.addAndGet(-freedSize);

                curCacheCount = cacheCount.addAndGet(-1);
            }
            cacheCount.addAndGet(1);

            long valueSize = calculateSize(file);
            long curCacheSize = cacheSize.get();
            while (curCacheSize + valueSize > sizeLimit) {
                long freedSize = removeNext();
                curCacheSize = cacheSize.addAndGet(-freedSize);
            }
            cacheSize.addAndGet(valueSize);

            Long currentTime = System.currentTimeMillis();
            if (file.setLastModified(currentTime)) {
                lastUsageDates.put(file, currentTime);
            }
        }

        private File get(String key) {
            File file = newFile(key);
            Long currentTime = System.currentTimeMillis();
            if (file.setLastModified(currentTime)) {
                lastUsageDates.put(file, currentTime);
            }

            return file;
        }

        private File newFile(String key) {
            return new File(cacheDir, key.hashCode() + "");
        }

        private boolean remove(String key) {
            File image = get(key);
            return image.delete();
        }
        private long removeNext() {
            if (lastUsageDates.isEmpty()) {
                return 0;
            }

            Long oldestUsage = null;
            File mostLongUsedFile = null;
            Set<Entry<File, Long>> entries = lastUsageDates.entrySet();
            synchronized (lastUsageDates) {
                for (Entry<File, Long> entry : entries) {
                    if (mostLongUsedFile == null) {
                        mostLongUsedFile = entry.getKey();
                        oldestUsage = entry.getValue();
                    } else {
                        Long lastValueUsage = entry.getValue();
                        if (lastValueUsage < oldestUsage) {
                            oldestUsage = lastValueUsage;
                            mostLongUsedFile = entry.getKey();
                        }
                    }
                }
            }

            long fileSize = calculateSize(mostLongUsedFile);
            if (mostLongUsedFile != null && mostLongUsedFile.delete()) {
                lastUsageDates.remove(mostLongUsedFile);
            }
            return fileSize;
        }

        private long calculateSize(File file) {
            return file.length();
        }


       private void clear() {
           lastUsageDates.clear();
           cacheSize.set(0);
           File[] files = cacheDir.listFiles();
           if (files != null) {
               for (File f : files) {
                   if (!f.delete()) {
                       break;
                   }
               }
           }
       }

    }


   /**
    * 保存 String数据 到 缓存中
    *
    * @param key   保存的key
    * @param value 保存的String数据
    */
   private void putString(String key, String value) {
       File file = mCache.newFile(key);
       BufferedWriter out = null;
       try {
           out = new BufferedWriter(new FileWriter(file), 1024);
           out.write(value);
       } catch (IOException e) {
           e.printStackTrace();
       } finally {
           if (out != null) {
               try {
                   out.flush();
                   out.close();
               } catch (IOException e) {
                   e.printStackTrace();
               }
           }
           mCache.put(file);
       }
   }

   private void putBitmap(String key, Bitmap value, int saveTime) {
       putByteArray(key, Utils.bitmap2Bytes(value), saveTime);
   }

   /**
    * 读取 bitmap 数据
    *
    * @return bitmap 数据
    */
   Bitmap getBitmap(String key) {
       if (getByteArray(key) == null) {
           return null;
       }
       return Utils.bytes2Bitmap(getByteArray(key));
   }


   /**
    * 保存 String数据 到 缓存中
    *
    * @param key      保存的key
    * @param value    保存的String数据
    * @param saveTime 保存的时间，单位：秒
    */
   private void putString(String key, String value, int saveTime) {
       putString(key, Utils.newStringWithDateInfo(saveTime, value));
   }

   /**
    * 读取 String数据
    *
    * @return String 数据
    */
   public String getString(String key) {
       File file = mCache.get(key);
       if (!file.exists()) {
           return null;
       }
       boolean removeFile = false;
       BufferedReader in = null;
       try {
           in = new BufferedReader(new FileReader(file));
           StringBuilder readString;
           String currentLine;
//
           readString = new StringBuilder(in.readLine());
           readString = new StringBuilder(readString.toString());
           while ((currentLine = in.readLine()) != null) {
               readString.append("\n").append(currentLine);
           }
           if (!Utils.isDue(readString.toString())) {
               return Utils.clearDateInfo(readString.toString());
           } else {
               removeFile = true;
               return null;
           }
       } catch (IOException e) {
           e.printStackTrace();
           return null;
       } finally {
           if (in != null) {
               try {
                   in.close();
               } catch (IOException e) {
                   e.printStackTrace();
               }
           }
           if (removeFile) {
               remove(key);
           }
       }
   }
   public void putBoolean(String key, boolean value) {
       putBoolean(key, value, -1);
   }

   private void putBoolean(String key, boolean value, int saveTime) {
       putObject(key, value, saveTime);
   }

   public boolean getBoolean(String key) {
       Object object = getObject(key);
       return object != null && (boolean) object;
   }

   public void putList(String key, List value) {
       putList(key, value, -1);
   }

   private void putList(String key, List value, int saveTime) {
       putObject(key, value, saveTime);
   }

   public List getList(String key) {
       return (List) getObject(key);
   }

   private void putObject(String key, Object value) {
       putObject(key, value, -1);
   }

   /**
    * 保存 Object到 缓存中
    *
    * @param key 保存的key
    * @param value 保存的value
    * @param saveTime 保存的时间，单位：秒
    */
   private void putObject(String key, Object value, int saveTime) {

       ObjectOutputStream oos = null;
       try {
           ByteArrayOutputStream bs = new ByteArrayOutputStream();
           oos = new ObjectOutputStream(bs);
           oos.writeObject(value);
           byte[] data = bs.toByteArray();
           if (saveTime != -1) {
               putByteArray(key, data, saveTime);
           } else {
               putByteArray(key, data);
           }
       } catch (Exception e) {
           e.printStackTrace();
       } finally {
           try {
               if (oos != null) {
                   oos.close();
               }
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
   }

   private Object getObject(String key) {
       byte[] data = getByteArray(key);
       if (data != null) {
           ByteArrayInputStream bais = null;
           ObjectInputStream ois = null;
           try {
               bais = new ByteArrayInputStream(data);
               ois = new ObjectInputStream(bais);
               return ois.readObject();
           } catch (Exception e) {
               e.printStackTrace();
               return null;
           } finally {
               try {
                   if (bais != null) {
                       bais.close();
                   }
               } catch (IOException e) {
                   e.printStackTrace();
               }
               try {
                   if (ois != null) {
                       ois.close();
                   }
               } catch (IOException e) {
                   e.printStackTrace();
               }
           }
       }
       return null;

   }
   public void putJSON(String key, JSONObject value) {
       putString(key, value.toString());
   }

   public void putJSON(String key, JSONObject value, int saveTime) {
       putString(key, value.toString(), saveTime);
   }

   public JSONObject getJSON(String key) {
       try {
           return new JSONObject(getString(key));
       } catch (Exception e) {
           e.printStackTrace();
           return null;
       }
   }


   public void putJSONArray(String key, JSONArray value) {
       putString(key, value.toString());
   }

   public void putJSONArray(String key, JSONArray value, int saveTime) {
       putString(key, value.toString(), saveTime);
   }

   public JSONArray getJSONArray(String key) {
       try {
           return new JSONArray(getString(key));
       } catch (Exception e) {
           e.printStackTrace();
           return null;
       }
   }
   /**
    * 保存 drawable 到 缓存中
    */
   public void putDrawable(String key, Drawable value) {
       putBitmap(key, Utils.drawable2Bitmap(value));
   }

   /**
    * 保存 drawable 到 缓存中
    *
    * @param key      保存的key
    * @param value    保存的 drawable 数据
    * @param saveTime 保存的时间，单位：秒
    */
   public void putDrawable(String key, Drawable value, int saveTime) {
       putBitmap(key, Utils.drawable2Bitmap(value), saveTime);
   }

   /**
    * 读取 Drawable 数据
    *
    * @return Drawable 数据
    */
   public Drawable getDrawable(String key) {
       if (getByteArray(key) == null) {
           return null;
       }
       return Utils.bitmap2Drawable(Utils.bytes2Bitmap(getByteArray(key)));
   }

   /**
    * 清除所有数据
    */
   public void clear() {
       mCache.clear();
   }
}
