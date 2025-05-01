package gitlet;

import java.io.File;
import java.util.*;
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
        Map<String, String> filenameToBlobRef = curCommit.getFilenameToBlobRef();
        if (filenameToBlobRef.containsKey(blobId) && filenameToBlobRef.get(filename).equals(blobId)) {
            curAddStageMap.remove(filename);
        } else if (curRemoveStageMap.containsKey(filename)) {
            curRemoveStageMap.remove(filename);
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
        Map<String, String> filenameToBlobRef = new HashMap<>();
        filenameToBlobRef.putAll(curAddStageMap);
        filenameToBlobRef.putAll(curRemoveStageMap);
        String curCommitId = getCurCommitId();
        List<String> parentRefs = new ArrayList<>();
        parentRefs.add(curCommitId);
        Commit commit = new Commit(message, filenameToBlobRef, parentRefs);
        commit.save();
        clearStage();
        File curBranchFile = join(HEADS_DIR, curBranch);
        writeContents(curBranchFile, commit.getId());
    }

    public static void rm(String filename) {
        File file = getFile(CWD, filename);
        getCurStage();
        curCommit = getCurCommit();
        Map<String, String> filenameToBlobRef = curCommit.getFilenameToBlobRef();
        if (curAddStageMap.containsKey(filename)) {
            curAddStageMap.remove(filename);
        } else if (filenameToBlobRef.containsKey(filename)) {
            if (file.exists()) {
                Blob blob = new Blob(filename, readContents(file));
                blob.save();
                curRemoveStageMap.put(filename, blob.getId());
                file.delete();
            } else
                curRemoveStageMap.put(filename, filenameToBlobRef.get(filename));
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
        Commit commit = getCurCommit();
        while (commit != null) {
            commit = displayCommit(commit);
        }
    }

    public static void globalLog() {
        List<String> filenames = plainFilenamesIn(OBJECTS_DIR);
        for (String filename : filenames) {
            try {
                File file = join(OBJECTS_DIR, filename);
                Commit commit = readObject(file, Commit.class);
                displayCommit(commit);
            } catch (Exception ignored) {
            }
        }
    }

    public static void find(String message) {
        boolean found = false;
        List<String> filenames = plainFilenamesIn(OBJECTS_DIR);
        for (String filename : filenames) {
            try {
                File file = join(OBJECTS_DIR, filename);
                Commit commit = readObject(file, Commit.class);
                if (commit.getMessage().equals(message)) {
                    System.out.println(commit.getId());
                    found = true;
                }
            } catch (Exception ignored) {
            }
        }
        if (!found) {
            exitWithMsg("Found no commit with that message.");
        }
    }

    public static void status() {
        System.out.println("=== Branches ===");
        curBranch = getCurBranch();
        List<String> branchNames = plainFilenamesIn(HEADS_DIR);
        for (String branchName : branchNames) {
            if (branchName.equals(curBranch)) {
                System.out.print("*");
            }
            System.out.println(branchName);
        }
        System.out.println("\n=== Staged Files ===");
        getCurStage();
        for (String stageFile : curAddStageMap.keySet()) {
            System.out.println(stageFile);
        }
        System.out.println("\n=== Removed Files ===");
        for (String stageFile : curRemoveStageMap.keySet()) {
            System.out.println(stageFile);
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        System.out.println("\n=== Untracked Files ===");
        Set<String> untrackedFilenames = getUntrackedFiles();
        for (String untrackedFilename : untrackedFilenames) {
            System.out.println(untrackedFilename);
        }
        System.out.println();
    }

    public static void checkout(String filename) {
        String commitId = getCurCommitId();
        restoreFileFromCommit(commitId, filename);
    }

    public static void checkout(String commitId, String filename) {
        restoreFileFromCommit(matchCommitId(commitId), filename);
    }

    public static void checkoutBranch(String branchName) {
        curBranch = getCurBranch();
        if (curBranch.equals(branchName)) {
            exitWithMsg("No need to checkout the current branch.");
        }
        String commitId = getLatestCommitIdOfBranch(branchName);
        restoreAllFilesFromCommit(commitId);
        writeContents(HEAD, branchName);
    }

    public static void branch(String branchName) {
        if (checkBranchIsExist(branchName)) {
            exitWithMsg("A branch with that name already exists.");
        }
        File file = join(HEADS_DIR, branchName);
        String commitId = getCurCommitId();
        writeContents(file, commitId);
    }

    public static void rmBranch(String branchName) {
        if (!checkBranchIsExist(branchName)) {
            exitWithMsg("A branch with that name does not exist.");
        }
        if (getCurBranch().equals(branchName)) {
            exitWithMsg("Cannot remove the current branch.");
        }
        File file = join(HEADS_DIR, branchName);
        file.delete();
    }

    public static void reset(String commitId) {
        commitId = matchCommitId(commitId);
        restoreAllFilesFromCommit(commitId);
        File file = join(HEADS_DIR, getCurBranch());
        writeContents(file, commitId);
        clearStage();
    }

    public static void checkIsFileUntracked() {
        Set<String> untrackedFilenames = getUntrackedFiles();
        if (!untrackedFilenames.isEmpty()) {
            exitWithMsg("There is an untracked file in the way; delete it, or add and commit it first.");
        }
    }

    public static boolean checkBranchIsExist(String branchName) {
        List<String> branchNames = plainFilenamesIn(HEADS_DIR);
        if (branchNames.contains(branchName)) {
            return true;
        }
        return false;
    }

    public static Set<String> getUntrackedFiles() {
        Set<String> untrackedFilenames = new HashSet<>(plainFilenamesIn(CWD));
        List<String> objectFilenames = plainFilenamesIn(OBJECTS_DIR);
        for (String blobFilename : objectFilenames) {
            try {
                File blobFile = join(OBJECTS_DIR, blobFilename);
                Blob blob = readObject(blobFile, Blob.class);
                String name = blob.getNameOfRawFile();
                if (untrackedFilenames.contains(name))
                    untrackedFilenames.remove(name);
            } catch (Exception ignored) {
            }
        }
        return untrackedFilenames;
    }

    public static void restoreAllFilesFromCommit(String commitId) {
        checkIsFileUntracked();
        Commit commit = getCommitById(commitId);
        Map<String, String> filenameToBlobRef = commit.getFilenameToBlobRef();
        for (String filename : filenameToBlobRef.keySet()) {
            restoreFileFromBlob(filenameToBlobRef.get(filename), filename);
        }
        curCommit = getCurCommit();
        Map<String, String> curFilenameToBlobRef = curCommit.getFilenameToBlobRef();
        for (String filename : curFilenameToBlobRef.keySet()) {
            if (!filenameToBlobRef.containsKey(filename)) {
                File file = join(CWD, filename);
                file.delete();
            }
        }
    }

    public static void restoreFileFromBlob(String blobId, String filename) {
        File fileToRead = join(OBJECTS_DIR, blobId);
        File fileToWrite = join(CWD, filename);
        Blob blob = readObject(fileToRead, Blob.class);
        writeContents(fileToWrite, blob.getBytes());
    }

    public static void restoreFileFromCommit(String commitId, String filename) {
        Commit commit = getCommitById(commitId);
        Map<String, String> filenameToBlobRef = commit.getFilenameToBlobRef();
        if (!filenameToBlobRef.containsKey(filename)) {
            exitWithMsg("File does not exist in that commit.");
        }
        restoreFileFromBlob(filenameToBlobRef.get(filename), filename);
    }

    public static String matchCommitId(String commitId) {
        List<String> filenames = plainFilenamesIn(OBJECTS_DIR);
        for (String filename : filenames) {
            if (filename.substring(0, commitId.length()).equals(commitId)) {
                return filename;
            }
        }
        exitWithMsg("No commit with that id exists.");
        return commitId;
    }

    public static Commit displayCommit(Commit commit) {
        List<String> parentRefs = commit.getParentRefs();
        System.out.println("===");
        System.out.println("commit " + commit.getId());
        if (parentRefs.size() > 1) {
            System.out.print("Merge:");
            for (String parent : parentRefs) {
                System.out.print(" " + parent.substring(0, 7));
            }
        }
        System.out.println("Date: " + commit.getTimestamp());
        System.out.println(commit.getMessage() + "\n");
        if (parentRefs.isEmpty())
            return null;
        return getCommitById(parentRefs.get(0));
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
        return getLatestCommitIdOfBranch(curBranch);
    }

    public static Commit getCommitById(String id) {
        File file = join(OBJECTS_DIR, id);
        if (!file.exists())
            return null;
        return readObject(file, Commit.class);
    }

    public static String getLatestCommitIdOfBranch(String branchName) {
        File file = join(HEADS_DIR, branchName);
        if (!file.exists())
            exitWithMsg("No such branch exists.");
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
