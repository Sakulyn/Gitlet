package gitlet;

import java.io.File;
import java.io.Serializable;

import static gitlet.Repository.OBJECTS_DIR;
import static gitlet.Utils.*;

/** Represents a gitlet blob object.
 *  blob 只与文件内容有关，与文件名、路径等元数据无关。
 *  blob 的 id 即 SHA-1 哈希值由文件内容计算得出。
 *  如果两个文件内容完全相同，gitlet 会复用该对象以节省空间。
 *
 *  @author Sakulyn
 */
public class Blob implements Serializable {
    private String id;
    private byte[] bytes;

    public Blob(byte[] bytes) {
        this.bytes = bytes;
        this.id = sha1(bytes);
    }

    public void save() {
        File file = join(OBJECTS_DIR, id);
        writeObject(file, this);
    }

    public String getId() {
        return id;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
