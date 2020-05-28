package gedi.analyzer;

import gedi.modules.ModuleBase;

public class AnalyzeCustom extends AnalyzerBase{
    public AnalyzeCustom() {

    }

    public void addModule(ModuleBase module) {
        modules.add(module);
    }
}
