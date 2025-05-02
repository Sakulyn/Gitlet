package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static gitlet.Repository.INDEX;
import static gitlet.Utils.writeObject;


public class Stage implements Serializable {
    private Map<String, String> addStageMap; // nameToBlobRefOfAddStage
    private Map<String, String> removeStageMap; // nameToBlobRefOfRemoveStage

    public Stage() {
        this.addStageMap = new HashMap<>();
        this.removeStageMap = new HashMap<>();
    }

    public Stage(Map<String, String> addStageMap, Map<String, String> removeStageMap) {
        this.addStageMap = addStageMap;
        this.removeStageMap = removeStageMap;
    }

    public void save() {
        writeObject(INDEX, this);
    }

    public Map<String, String> getAddStageMap() {
        return addStageMap;
    }

    public Map<String, String> getRemoveStageMap() {
        return removeStageMap;
    }
}
