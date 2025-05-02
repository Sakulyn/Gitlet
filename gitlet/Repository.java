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
        if (checkStageIsEmpty()) {
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
                Commit commit = readCommit(filename);
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
                Commit commit = readCommit(filename);
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
        if (!checkStageIsEmpty()) {
            exit("You have uncommitted changes.");
        }
        if (!checkBranchIsExist(branchName)) {
            exit("A branch with that name does not exist.");
        }
        getCurBranch();
        if (curBranch.equals(branchName)) {
            exit("Cannot merge a branch with itself.");
        }
        getCurCommit();
        String curCommitId = curCommit.getId();
        String otherCommitId = getLatestCommitIdOfBranch(branchName);
        Map<String, String> curMap = curCommit.getFilenameToBlobRef();
        Map<String, String> otherMap = getCommitById(otherCommitId).getFilenameToBlobRef();
        checkUntrackedFiles(curMap, otherMap);
        String splitPointId = findSplitPoint(curCommitId, otherCommitId);
        if (splitPointId.equals(curCommitId)) {
            checkoutBranch(branchName);
            System.out.println("Current branch fast-forwarded.");
        }
        Map<String, String> splitMap = getCommitById(splitPointId).getFilenameToBlobRef();
        Map<String, String> map = new HashMap<>();
        for (String filename : splitMap.keySet()) {
            String blobId = readBlob(splitMap.get(filename)).getId();
            boolean fileTrackedByCurCommit = curMap.containsKey(filename);
            boolean fileTrackedByOtherCommit = otherMap.containsKey(filename);
            if (fileTrackedByCurCommit && fileTrackedByOtherCommit) {
                String curBlobId = curMap.get(filename);
                String otherBlobId = otherMap.get(filename);
                map.put(filename, blobId);
                if (!curBlobId.equals(blobId) && !otherBlobId.equals(blobId)) {
                    map.put(filename, mergeConflictFiles(curBlobId, otherBlobId));
                } else if (!curMap.get(filename).equals(blobId)) {
                    map.put(filename, curMap.get(filename));
                } else if (!otherMap.get(filename).equals(blobId)) {
                    map.put(filename, otherMap.get(filename));
                }
            } else if (!fileTrackedByCurCommit && hasKeyButNotValue(otherMap, filename, blobId)) {
                map.put(filename, mergeConflictFiles(null, otherMap.get(filename)));
            } else if (!fileTrackedByOtherCommit && hasKeyButNotValue(curMap, filename, blobId)) {
                map.put(filename, mergeConflictFiles(curMap.get(filename), null));
            }
        }
        for (String filename : otherMap.keySet()) {
            if (!splitMap.containsKey(filename)) {
                if (!curMap.containsKey(filename)) {
                    map.put(filename, otherMap.get(filename));
                } else if (hasKeyButNotValue(curMap, filename, otherMap.get(filename))) {
                    String curBlobId = curMap.get(filename);
                    String otherBlobId = otherMap.get(filename);
                    map.put(filename, mergeConflictFiles(curBlobId, otherBlobId));
                }
            }
        }
        for (String filename : curMap.keySet()) {
            if (!splitMap.containsKey(filename) && !otherMap.containsKey(filename)) {
                map.put(filename, curMap.get(filename));
            }
        }
        Set<String> workingFilenames = new HashSet<>(plainFilenamesIn(CWD));
        for (String filename : workingFilenames) {
            if (!map.containsKey(filename)) {
                restrictedDelete(join(CWD, filename));
            }
        }
        for (String filename : map.keySet()) {
            restoreFileFromBlob(filename, map.get(filename));
        }
        String message = "Merged " + branchName + " into " + curBranch + ".";
        List<String> parentRefs = new ArrayList<>();
        parentRefs.add(curCommitId);
        parentRefs.add(otherCommitId);
        Commit commit = new Commit(message, map, parentRefs);
        commit.save();
        writeContents(join(HEADS_DIR, curBranch), commit.getId());
    }

    public static String mergeConflictFiles(String curBlobId, String otherBlobId) {
        if (curBlobId.equals(otherBlobId)) {
            return curBlobId;
        }
        String curContent = "";
        String otherContent = "";
        if (curBlobId != null) {
            Blob curBlob = readBlob(curBlobId);
            curContent = new String(curBlob.getBytes());
        }
        if (otherBlobId != null) {
            Blob otherBlob = readBlob(otherBlobId);
            otherContent = new String(otherBlob.getBytes());
        }
        String conflictContent = conflictContentStyle(curContent, otherContent);
        Blob confilctBlob = new Blob(conflictContent.getBytes());
        confilctBlob.save();
        return confilctBlob.getId();
    }

    public static String conflictContentStyle(String curContent, String otherContent) {
        System.out.println("Encountered a merge conflict. ");
        return "<<<<<<< HEAD\n" + curContent + "=======\n" + otherContent + ">>>>>>>\n";
    }

    public static String findSplitPoint(String commitIdA, String commitIdB) {
        if (commitIdA.equals(commitIdB)) {
            return commitIdA;
        }
        String splitPointId = commitIdA;
        Map<String, Integer> treeA = dfsCommitTree(commitIdA, new HashMap<>()); // id to depth
        Map<String, Integer> treeB = dfsCommitTree(commitIdB, new HashMap<>());
        int splitPointDepth = Integer.MAX_VALUE;
        for (String commitId : treeA.keySet()) {
            if (commitId.equals(commitIdB)) {
                exit("Given branch is an ancestor of the current branch.");
            } else if (treeB.containsKey(commitId) && treeA.get(commitId) < splitPointDepth) {
                splitPointId = commitId;
                splitPointDepth = treeA.get(commitId);
            }
        }
        return splitPointId;
    }

    public static Map<String, Integer> dfsCommitTree(String commitId, Map<String, Integer> map) {
        Commit commit = getCommitById(commitId);
        if (map.isEmpty()) {
            map.put(commit.getId(), 0);
        }
        List<String> parentRefs = commit.getParentRefs();
        if (parentRefs.isEmpty()) {
            return null;
        }
        String parentIdA = parentRefs.get(0);
        map.put(parentIdA, map.get(commitId) + 1);
        Map<String, Integer> parentTreeA = dfsCommitTree(parentIdA, map);
        if (parentTreeA != null) {
            map.putAll(parentTreeA);
        }
        if (parentRefs.size() == 2) {
            String parentIdB = parentRefs.get(1);
            map.put(parentIdB, map.get(commitId) + 1);
            Map<String, Integer> parentTreeB = dfsCommitTree(parentIdB, map);
            if (parentTreeB != null) {
                map.putAll(parentTreeB);
            }
        }
        return map;
    }

    public static Blob readBlob(String blobId) {
        return readObject(join(OBJECTS_DIR, blobId), Blob.class);
    }

    public static Commit readCommit(String commitId) {
        return readObject(join(OBJECTS_DIR, commitId), Commit.class);
    }

    public static boolean hasKeyEqualsValue(Map<String, String> map, String key, String val) {
        return map.containsKey(key) && map.get(key).equals(val);
    }

    public static boolean hasKeyButNotValue(Map<String, String> map, String key, String val) {
        return map.containsKey(key) && !map.get(key).equals(val);
    }

    public static boolean checkStageIsEmpty() {
        getCurStage();
        return curAddStageMap.isEmpty() && curRmStageMap.isEmpty();
    }

    public static boolean checkBranchIsExist(String branchName) {
        List<String> branchNames = plainFilenamesIn(HEADS_DIR);
        return branchNames.contains(branchName);
    }

    public static void checkUntrackedFiles(Map<String, String> cur, Map<String, String> other) {
        Set<String> workingFiles = new HashSet<>(plainFilenamesIn(CWD));
        boolean isFileTrackedByOtherCommitNotByCurCommit = false;
        for (String filename : other.keySet()) {
            if (workingFiles.contains(filename) && !cur.containsKey(filename)) {
                isFileTrackedByOtherCommitNotByCurCommit = true;
                break;
            }
        }
        if (isFileTrackedByOtherCommitNotByCurCommit) {
            exit("There is an untracked file in the way; delete it, or add and commit it first.");
        }
    }

    public static void restoreAllFilesFromCommit(String commitId) {
        getCurCommit();
        Commit otherCommit = getCommitById(commitId);
        Map<String, String> filesTrackedByCurCommit = curCommit.getFilenameToBlobRef();
        Map<String, String> filesTrackedByOtherCommit = otherCommit.getFilenameToBlobRef();
        checkUntrackedFiles(filesTrackedByCurCommit, filesTrackedByOtherCommit);
        for (String filename : filesTrackedByCurCommit.keySet()) {
            if (!filesTrackedByOtherCommit.containsKey(filename)) {
                restrictedDelete(join(CWD, filename));
            }
        }
        for (String filename : filesTrackedByOtherCommit.keySet()) {
            restoreFileFromBlob(filename, filesTrackedByOtherCommit.get(filename));
        }
    }

    public static void restoreFileFromBlob(String filename, String blobId) {
        Blob blob = readBlob(blobId);
        File fileToWrite = join(CWD, filename);
        writeContents(fileToWrite, blob.getBytes());
    }

    public static void restoreFileFromCommit(String commitId, String filename) {
        Commit commit = getCommitById(commitId);
        Map<String, String> filenameToBlobRef = commit.getFilenameToBlobRef();
        if (!filenameToBlobRef.containsKey(filename)) {
            exit("File does not exist in that commit.");
        }
        restoreFileFromBlob(filename, filenameToBlobRef.get(filename));
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
            System.out.println();
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
