package gitlet;

import java.io.File;
import java.io.Serializable;

import static gitlet.Repository.OBJECTS_DIR;
import static gitlet.Utils.*;

public class Blob implements Serializable {
    private String id;
    private String path;
    private byte[] bytes;
    public Blob(String path, byte[] bytes) {
        this.path = path;
        this.bytes = bytes;
        this.id = generateId();
    }

    public void save() {
        File file = join(OBJECTS_DIR, id);
        writeObject(file, this);
    }
    public String generateId() {
        return sha1(path, bytes.toString());
    }
    public String getId() {
        return id;
    }
}
