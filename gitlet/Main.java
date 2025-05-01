package gitlet;

/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 *
 * @author Sakulyn
 */
public class Main {
    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        int inputSize = args.length;
        if (inputSize < 1) {
            exitWithMsg("Please enter a command.");
        }
        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                // TODO: handle the `init` command
                if (inputSize > 1) {
                    exitWithMsg("Incorrect operands.");
                }
                Repository.init();
                break;
            case "add":
                // TODO: handle the `add [filename]` command
                checkNumberOfOperands(inputSize, 2);
                Repository.add(args[1]);
                break;
            // TODO: FILL THE REST IN
            case "commit":
                checkNumberOfOperands(inputSize, 2);
                Repository.commit(args[1]);
                break;
            case "rm":
                checkNumberOfOperands(inputSize, 2);
                Repository.rm(args[1]);
                break;
            case "log":
                checkNumberOfOperands(inputSize, 1);
                Repository.log();
                break;
            case "global-log":
                checkNumberOfOperands(inputSize, 1);
                Repository.globalLog();
                break;
            case "find":
                checkNumberOfOperands(inputSize, 2);
                Repository.find(args[1]);
                break;
            case "status":
                checkNumberOfOperands(inputSize, 1);
                Repository.status();
                break;
            case "checkout":
                checkInitialization();
                boolean isCorrectFormatOfOperands = true;
                if (inputSize == 3) {
                    if (!args[1].equals("--")) {
                        isCorrectFormatOfOperands = false;
                    } else {
                        Repository.checkout(args[2]);
                    }
                } else if (inputSize == 4) {
                    if (!args[2].equals("--")) {
                        isCorrectFormatOfOperands = false;
                    } else {
                        Repository.checkout(args[1], args[3]);
                    }
                } else if (inputSize == 2) {
                    Repository.checkoutBranch(args[1]);
                } else {
                    isCorrectFormatOfOperands = false;
                }
                if (!isCorrectFormatOfOperands) {
                    exitWithMsg("Incorrect operands.");
                }
                break;
            case "branch":
                checkNumberOfOperands(inputSize, 2);
                Repository.branch(args[1]);
                break;
            case "rm-branch":
                checkNumberOfOperands(inputSize, 2);
                Repository.rmBranch(args[1]);
                break;
            case "reset":
                checkNumberOfOperands(inputSize, 2);
                Repository.reset(args[1]);
                break;
            default:
                exitWithMsg("No command with that name exists.");
        }
    }

    public static void checkNumberOfOperands(int input, int target) {
        checkInitialization();
        if (input != target) {
            exitWithMsg("Incorrect operands.");
        }
    }

    public static void checkInitialization() {
        if (!Repository.GITLET_DIR.exists())
            exitWithMsg("Not in an initialized Gitlet directory.");
    }

    public static void exitWithMsg(String msg) {
        System.out.println(msg);
        System.exit(0);
    }
}
