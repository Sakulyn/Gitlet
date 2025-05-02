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
        int inputSize = args.length;
        if (inputSize < 1) {
            exit("Please enter a command.");
        }
        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                if (inputSize > 1) {
                    exit("Incorrect operands.");
                }
                Repository.init();
                break;
            case "add":
                checkNumberOfOperands(inputSize, 2);
                Repository.add(args[1]);
                break;
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
                if (inputSize == 3 && args[1].equals("--")) {
                    Repository.checkout(args[2]);
                } else if (inputSize == 4 && args[2].equals("--")) {
                    Repository.checkout(args[1], args[3]);
                } else if (inputSize == 2) {
                    Repository.checkoutBranch(args[1]);
                } else {
                    exit("Incorrect operands.");
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
            case "merge":
                checkNumberOfOperands(inputSize, 2);
                Repository.merge(args[1]);
                break;
            default:
                exit("No command with that name exists.");
        }
    }

    public static void checkNumberOfOperands(int input, int target) {
        checkInitialization();
        if (input != target) {
            exit("Incorrect operands.");
        }
    }

    public static void checkInitialization() {
        if (!Repository.GITLET_DIR.exists())
            exit("Not in an initialized Gitlet directory.");
    }

    public static void exit(String msg) {
        System.out.println(msg);
        System.exit(0);
    }
}
