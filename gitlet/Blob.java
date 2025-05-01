package gitlet;

import java.io.File;
import java.io.Serializable;

import static gitlet.Repository.OBJECTS_DIR;
import static gitlet.Utils.*;

public class Blob implements Serializable {
    private String id;
    private String nameOfRawFile;
    private byte[] bytes;

    public Blob(String nameOfRawFile, byte[] bytes) {
        this.nameOfRawFile = nameOfRawFile;
        this.bytes = bytes;
        this.id = generateId();
    }

    public void save() {
        File file = join(OBJECTS_DIR, id);
        writeObject(file, this);
    }

    public String generateId() {
        return sha1(nameOfRawFile, bytes);
    }

    public String getId() {
        return id;
    }

    public String getNameOfRawFile() {
        return nameOfRawFile;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
