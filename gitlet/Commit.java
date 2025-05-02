package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Repository.OBJECTS_DIR;
import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *
 *  @author Sakulyn
 */
public class Commit implements Serializable {
    /** The message of this Commit. */
    private String message; // log message
    private String timestamp; // commit time
    private String id; // Sha-1 id
    private Map<String, String> filenameToBlobRef;  // 文件名（不考虑子目录下文件）到 blob 引用的映射
    private List<String> parentRefs;

    public Commit() {
        filenameToBlobRef = new HashMap<>();
        parentRefs = new ArrayList<>();
        message = "initial commit";
        timestamp = dateToTimestamp(new Date(0));
        id = generateId();
    }

    public Commit(String message, Map<String, String> filenameToBlobRef, List<String> parentRefs) {
        this.filenameToBlobRef = filenameToBlobRef;
        this.parentRefs = parentRefs;
        this.message = message;
        this.timestamp = dateToTimestamp(new Date());
        this.id = generateId();
    }

    public void save() {
        File file = join(OBJECTS_DIR, id);
        writeObject(file, this);
    }

    public String dateToTimestamp(Date date) {
        DateFormat df = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return df.format(date);
    }

    public String generateId() {
        return sha1(filenameToBlobRef.toString(), parentRefs.toString(), message, timestamp);
    }

    public Map<String, String> getFilenameToBlobRef() {
        return filenameToBlobRef;
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public List<String> getParentRefs() {
        return parentRefs;
    }
}
