package gitlet;

import java.io.File;

import static gitlet.Main.exitWithMsg;
import static gitlet.Utils.*;

// TODO: any imports you need here

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
}
