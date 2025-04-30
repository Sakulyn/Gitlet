package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
// TODO: any imports you need here
import static gitlet.Main.exitWithMsg;
import static gitlet.Utils.*;

/**
 * Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 * @author Sakulyn
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /**
     * CWD                              <==== The current working directory.
     * └── .gitlet                      <==== The .gitlet directory.
     *      ├── HEAD
     *      ├── objects
     *      ├── index
     *      └── refs
     *          └── heads
     *              ├── master
     *              ├── ...
     *              └── other branch
     *          └── remotes
     *              └── origin
     *                  └── HEAD
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File HEAD = join(GITLET_DIR, "HEAD");
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    public static final File INDEX = join(GITLET_DIR, "index");
    public static final File REFS_DIR = join(GITLET_DIR, "refs");
    public static final File HEADS_DIR = join(REFS_DIR, "heads");
    public static String curBranch = "master";
    public static Commit curCommit;
    public static Map<String, String> curAddStageMap;
    public static Map<String, String> curRemoveStageMap;

    /* TODO: fill in the rest of this class. */
    public static void init() {
        if (GITLET_DIR.exists())
            exitWithMsg("A Gitlet version-control system already exists in the current directory.");
        GITLET_DIR.mkdir();
        OBJECTS_DIR.mkdir();
        HEADS_DIR.mkdirs();
        File master = join(HEADS_DIR, "master");
        Commit commit = new Commit();
        commit.save();
        writeContents(HEAD, curBranch);
        writeContents(master, commit.getId());
    }

    public static void add(String filename) {
        File file = getFile(CWD, filename);
        getCurStage();
        curCommit = getCurCommit();
        Blob blob = new Blob(filename, readContents(file));
        String blobId = blob.getId();
        Map<String, String> pathToBlobRef = curCommit.getPathToBlobRef();
        if (pathToBlobRef.containsKey(blobId) && pathToBlobRef.get(filename).equals(blobId)) {
            curAddStageMap.remove(filename);
        } else {
            blob.save();
            curAddStageMap.put(filename, blobId);
        }
        Stage stage = new Stage(curAddStageMap, curRemoveStageMap);
        stage.save();
    }

    public static void commit(String message) {
        getCurStage();
        if (curAddStageMap.isEmpty() && curRemoveStageMap.isEmpty()) {
            exitWithMsg("No changes added to the commit.");
        }
        if (message.isEmpty()) {
            exitWithMsg("Please enter a commit message.");
        }
        Map<String, String> pathToBlobRef = curAddStageMap;
        pathToBlobRef.putAll(curRemoveStageMap);
        String curCommitId = getCurCommitId();
        List<String> parentRefs = new ArrayList<>();
        parentRefs.add(curCommitId);
        Commit commit = new Commit(message, pathToBlobRef, parentRefs);
        commit.save();
        clearStage();
        File curBranchFile = join(HEADS_DIR, curBranch);
        writeContents(curBranchFile, commit.getId());
    }

    public static void rm(String filename) {
        File file = getFile(CWD, filename);
        getCurStage();
        curCommit = getCurCommit();
        Map<String, String> pathToBlobRef = curAddStageMap;
        if (curAddStageMap.containsKey(filename)) {
            curAddStageMap.remove(filename);
        } else if (pathToBlobRef.containsKey(filename)) {
            if (file.exists()) {
                Blob blob = new Blob(filename, readContents(file));
                blob.save();
                curRemoveStageMap.put(filename, blob.getId());
                file.delete();
            } else
                curRemoveStageMap.put(filename, pathToBlobRef.get(filename));
        } else {
            exitWithMsg("No reason to remove the file.");
        }
        Stage stage = new Stage(curAddStageMap, curRemoveStageMap);
        stage.save();
    }

    /**
     * format
     * ===
     * commit e881c9575d180a215d1a636545b8fd9abfb1d2bb
     * Date: Wed Dec 31 16:00:00 1969 -0800
     * initial commit
     *
     */
    public static void log() {
        curCommit = getCurCommit();
        displayCommitTree(curCommit);
    }

    public static void displayCommitTree(Commit commit) {
        while (commit != null) {
            List<String> parentRefs = commit.getParentRefs();
            System.out.println("===");
            System.out.println("commit " + commit.getId());
            if (parentRefs.size() > 1) {
                System.out.printf("Merge:");
                for (String parent : parentRefs) {
                    System.out.printf(" " + parent.substring(7));
                }
            }
            System.out.println("Date: " + commit.getTimestamp());
            System.out.println(commit.getMessage() + "\n");
            if(parentRefs.size() == 0)
                break;
            commit = getCommitById(parentRefs.get(0));
        }
    }

    public static void clearStage() {
        Stage stage = new Stage();
        stage.save();
    }

    public static void getCurStage() {
        Stage curStage = new Stage();
        if (INDEX.exists()) {
            curStage = readObject(INDEX, Stage.class);
        }
        curAddStageMap = curStage.getAddStageMap();
        curRemoveStageMap = curStage.getRemoveStageMap();
    }

    public static Commit getCurCommit() {
        String commitId = getCurCommitId();
        return getCommitById(commitId);
    }

    public static String getCurCommitId() {
        curBranch = getCurBranch();
        String commitId = getLatestCommitIdOfBranch(curBranch);
        return commitId;
    }

    public static Commit getCommitById(String id) {
        File file = join(OBJECTS_DIR, id);
        if (!file.exists())
            return null;
        return readObject(file, Commit.class);
    }

    public static String getLatestCommitIdOfBranch(String branchName) {
        File file = join(HEADS_DIR, branchName);
        return readContentAsString(file);
    }

    public static String getCurBranch() {
        return readContentAsString(HEAD);
    }

    public static String readContentAsString(File file) {
        return bytesToString(readContents(file));
    }

    public static String bytesToString(byte[] bytes) {
        return new String(bytes);
    }

    public static void checkFileIsExist(File file) {
        if (!file.exists()) {
            exitWithMsg("File does not exist.");
        }
    }

    public static File getFile(File dir, String filename) {
        File file = join(dir, filename);
        checkFileIsExist(file);
        return file;
    }
}
