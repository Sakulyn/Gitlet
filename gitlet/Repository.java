package gitlet;

import java.io.File;
import java.util.*;

import static gitlet.Main.exit;
import static gitlet.Utils.*;

/**
 * Represents a gitlet repository.
 *
 * @author Sakulyn
 */
public class Repository {
    /**
     * CWD                              <==== The current working directory.
     * └── .gitlet                      <==== The .gitlet directory.
     *      ├── HEAD
     *      ├── objects                 <==== The objects directory stores all commits and blobs.
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
    private static String curBranch = "master";
    private static Commit curCommit;
    private static Map<String, String> curAddStageMap;
    private static Map<String, String> curRmStageMap;

    public static void init() {
        if (GITLET_DIR.exists()) {
            exit("A Gitlet version-control system already exists in the current directory.");
        }
        OBJECTS_DIR.mkdirs();
        HEADS_DIR.mkdirs();
        Commit commit = new Commit();
        commit.save();
        writeContents(HEAD, curBranch);
        File master = join(HEADS_DIR, curBranch);
        writeContents(master, commit.getId());
    }

    public static void add(String filename) {
        File workingFile = join(CWD, filename);
        if (!workingFile.exists()) {
            exit("File does not exist.");
        }
        getCurStage();
        Blob blob = new Blob(readContents(workingFile));
        String blobId = blob.getId();
        Map<String, String> curNameToBlobMap = getCurCommit().getFilenameToBlobRef();
        boolean rmStageContainFile = hasKeyEqualsValue(curRmStageMap, filename, blobId);
        if (rmStageContainFile) {
            curRmStageMap.remove(filename);
        }
        if (hasKeyEqualsValue(curNameToBlobMap, filename, blobId)) {
            curAddStageMap.remove(filename);
        } else if (!rmStageContainFile) {
            blob.save();
            curAddStageMap.put(filename, blobId);
        }
        Stage stage = new Stage(curAddStageMap, curRmStageMap);
        stage.save();
    }

    public static void commit(String message) {
        getCurStage();
        if (curAddStageMap.isEmpty() && curRmStageMap.isEmpty()) {
            exit("No changes added to the commit.");
        }
        if (message.isEmpty()) {
            exit("Please enter a commit message.");
        }
        getCurCommit();
        Map<String, String> filenameToBlobRef = curCommit.getFilenameToBlobRef();
        filenameToBlobRef.putAll(curAddStageMap);
        for (String filename : curRmStageMap.keySet()) {
            filenameToBlobRef.remove(filename);
        }
        List<String> parentRefs = new ArrayList<>();
        parentRefs.add(curCommit.getId());
        Commit commit = new Commit(message, filenameToBlobRef, parentRefs);
        commit.save();
        clearStage();
        File curBranchFile = join(HEADS_DIR, curBranch);
        writeContents(curBranchFile, commit.getId());
    }

    public static void rm(String filename) {
        getCurStage();
        getCurCommit();
        File workingFile = join(CWD, filename);
        Map<String, String> curNameToBlobMap = curCommit.getFilenameToBlobRef();
        if (curAddStageMap.containsKey(filename)) {
            curAddStageMap.remove(filename);
        } else if (curNameToBlobMap.containsKey(filename)) {
            if (workingFile.exists()) {
                Blob blob = new Blob(readContents(workingFile));
                blob.save();
                curRmStageMap.put(filename, blob.getId());
                restrictedDelete(workingFile);
            } else {
                curRmStageMap.put(filename, curNameToBlobMap.get(filename));
            }
        } else {
            exit("No reason to remove the file.");
        }
        Stage stage = new Stage(curAddStageMap, curRmStageMap);
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
        List<String> objFilenames = plainFilenamesIn(OBJECTS_DIR);
        for (String filename : objFilenames) {
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
        List<String> objFilenames = plainFilenamesIn(OBJECTS_DIR);
        for (String filename : objFilenames) {
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
            exit("Found no commit with that message.");
        }
    }

    public static void status() {
        System.out.println("=== Branches ===");
        getCurCommit();
        List<String> branchNames = plainFilenamesIn(HEADS_DIR);
        for (String branchName : branchNames) {
            if (branchName.equals(curBranch)) {
                System.out.print("*");
            }
            System.out.println(branchName);
        }
        System.out.println("\n=== Staged Files ===");
        getCurStage();
        for (String stageForAdditionFilename : curAddStageMap.keySet()) {
            System.out.println(stageForAdditionFilename);
        }
        System.out.println("\n=== Removed Files ===");
        for (String stageForRemovalFilename : curRmStageMap.keySet()) {
            System.out.println(stageForRemovalFilename);
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        Set<String> workingFilenames = new HashSet<>(plainFilenamesIn(CWD));
        Map<String, String> curnameToBlobMap = curCommit.getFilenameToBlobRef();
        for (String filename : workingFilenames) {
            Blob blob = new Blob(readContents(join(CWD, filename)));
            String blobId = blob.getId();
            boolean isModified = hasKeyButNotValue(curnameToBlobMap, filename, blobId);
            isModified = isModified && !curAddStageMap.containsKey(filename);
            isModified = isModified || hasKeyButNotValue(curAddStageMap, filename, blobId);
            if (isModified) {
                System.out.println(filename + " (modified)");
            }
        }
        for (String filename : curAddStageMap.keySet()) {
            if (!workingFilenames.contains(filename)) {
                System.out.println(filename + " (deleted)");
            }
        }
        for (String filename : curnameToBlobMap.keySet()) {
            if (!curRmStageMap.containsKey(filename) && !workingFilenames.contains(filename)) {
                System.out.println(filename + " (deleted)");
            }
        }
        System.out.println("\n=== Untracked Files ===");
        workingFilenames.removeAll(curAddStageMap.keySet());
        workingFilenames.removeAll(curnameToBlobMap.keySet());
        for (String untrackedFilename : workingFilenames) {
            System.out.println(untrackedFilename);
        }
        System.out.println();
    }

    public static void checkout(String filename) {
        restoreFileFromCommit(getCurCommitId(), filename);
    }

    public static void checkout(String commitId, String filename) {
        restoreFileFromCommit(matchCommitId(commitId), filename);
    }

    public static void checkoutBranch(String branchName) {
        getCurBranch();
        if (curBranch.equals(branchName)) {
            exit("No need to checkout the current branch.");
        }
        String commitId = getLatestCommitIdOfBranch(branchName);
        restoreAllFilesFromCommit(commitId);
        writeContents(HEAD, branchName);
        clearStage();
    }

    public static void branch(String branchName) {
        if (checkBranchIsExist(branchName)) {
            exit("A branch with that name already exists.");
        }
        File file = join(HEADS_DIR, branchName);
        writeContents(file, getCurCommitId());
    }

    public static void rmBranch(String branchName) {
        if (!checkBranchIsExist(branchName)) {
            exit("A branch with that name does not exist.");
        }
        if (getCurBranch().equals(branchName)) {
            exit("Cannot remove the current branch.");
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

    /**
     * modified in other but not HEAD ===> other
     * modified in HEAD but not other ===> HEAD
     * modified in other and HEAD ===> conflict (in diff ways);
     * not in split nor other but in HEAD  ===> HEAD
     * not in split nor HEAD but in other ===> other
     * unmodified in HEAD but not present in other ===> remove
     * unmodified in other but not present in HEAD ===> remain removed
     */
    public static void merge(String branchName) {
        getCurStage();
        if (!curAddStageMap.isEmpty() || !curRmStageMap.isEmpty()) {
            exit("You have uncommitted changes.");
        }
        if (!checkBranchIsExist(branchName)) {
            exit("A branch with that name does not exist.");
        }
        getCurBranch();
        if (curBranch.equals(branchName)) {
            exit("Cannot merge a branch with itself.");
        }
        String curCommitId = getCurCommitId();
        String otherCommitId = getLatestCommitIdOfBranch(branchName);
        String splitPointId = findSplitPoint(curCommitId, otherCommitId);
        if (splitPointId.equals(curCommitId)) {
            exit("Current branch fast-forwarded.");
        }
    }

    public static String findSplitPoint(String commitIdA, String commitIdB) {
        if (commitIdA.equals(commitIdB)) {
            return commitIdA;
        }
        Commit commitA = getCommitById(commitIdA);
        Commit commitB = getCommitById(commitIdB);
        Set<String> commitTreeA = new HashSet<>();
        while (commitA != null) {
            String parentCommitId = getParentCommitId(commitA);
            if (parentCommitId != null) {
                if (parentCommitId.equals(commitIdB)) {
                    exit("Given branch is an ancestor of the current branch.");
                }
                commitTreeA.add(parentCommitId);
            }
            commitA = getCommitById(parentCommitId);
        }
        while (commitB != null) {
            String parentCommitId = getParentCommitId(commitB);
            if (parentCommitId != null && commitTreeA.contains(parentCommitId)) {
                return parentCommitId;
            }
            commitB = getCommitById(parentCommitId);
        }
        return null;
    }

    public static String getParentCommitId(Commit commit) {
        List<String> parentRefs = commit.getParentRefs();
        if (parentRefs.isEmpty()) {
            return null;
        }
        return parentRefs.get(0);
    }

    public static boolean hasKeyEqualsValue(Map<String, String> map, String key, String val) {
        return map.containsKey(key) && map.get(key).equals(val);
    }

    public static boolean hasKeyButNotValue(Map<String, String> map, String key, String val) {
        return map.containsKey(key) && !map.get(key).equals(val);
    }

    public static boolean checkBranchIsExist(String branchName) {
        List<String> branchNames = plainFilenamesIn(HEADS_DIR);
        if (branchNames.contains(branchName)) {
            return true;
        }
        return false;
    }

    public static void restoreAllFilesFromCommit(String commitId) {
        getCurCommit();
        Commit otherCommit = getCommitById(commitId);
        Set<String> workingFiles = new HashSet<>(plainFilenamesIn(CWD));
        Map<String, String> filesTrackedByCurCommit = curCommit.getFilenameToBlobRef();
        Map<String, String> filesTrackedByOtherCommit = otherCommit.getFilenameToBlobRef();
        boolean isFileTrackedByOtherCommitNotByCurCommit = false;
        for (String filename : filesTrackedByOtherCommit.keySet()) {
            if (workingFiles.contains(filename) && !filesTrackedByCurCommit.containsKey(filename)) {
                isFileTrackedByOtherCommitNotByCurCommit = true;
                break;
            }
        }
        if (isFileTrackedByOtherCommitNotByCurCommit) {
            exit("There is an untracked file in the way; delete it, or add and commit it first.");
        }
        for (String filename : filesTrackedByCurCommit.keySet()) {
            if (!filesTrackedByOtherCommit.containsKey(filename)) {
                restrictedDelete(join(CWD, filename));
            }
        }
        for (String filename : filesTrackedByOtherCommit.keySet()) {
            restoreFileFromBlob(filesTrackedByOtherCommit.get(filename), filename);
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
            exit("File does not exist in that commit.");
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
        exit("No commit with that id exists.");
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
        if (parentRefs.isEmpty()) {
            return null;
        }
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
        curRmStageMap = curStage.getRemoveStageMap();
    }

    public static Commit getCurCommit() {
        String commitId = getCurCommitId();
        curCommit = getCommitById(commitId);
        return curCommit;
    }

    public static String getCurCommitId() {
        getCurBranch();
        return getLatestCommitIdOfBranch(curBranch);
    }

    public static Commit getCommitById(String id) {
        if (id == null) {
            return null;
        }
        File file = join(OBJECTS_DIR, id);
        if (!file.exists()) {
            return null;
        }
        return readObject(file, Commit.class);
    }

    public static String getLatestCommitIdOfBranch(String branchName) {
        File file = join(HEADS_DIR, branchName);
        if (!file.exists()) {
            exit("No such branch exists.");
        }
        return readContentsAsString(file);
    }

    public static String getCurBranch() {
        curBranch = readContentsAsString(HEAD);
        return curBranch;
    }

}
